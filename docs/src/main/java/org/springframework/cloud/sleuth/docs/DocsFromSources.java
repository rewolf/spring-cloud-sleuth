/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.Expression;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.ReturnStatement;
import org.jboss.forge.roaster._shade.org.eclipse.jdt.core.dom.StringLiteral;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.JavaUnit;
import org.jboss.forge.roaster.model.impl.JavaEnumImpl;
import org.jboss.forge.roaster.model.source.EnumConstantSource;
import org.jboss.forge.roaster.model.source.MemberSource;

public class DocsFromSources {

	private static final Log logger = LogFactory.getLog(DocsFromSources.class);

	private static final String ADOC_HEADER = ".Spring Cloud Sleuth Tags\n" + "|===\n" + "|Name | Description\n";

	private final File projectRoot;

	private final String inclusionPattern;

	private final File outputDir;

	public DocsFromSources(File projectRoot, String inclusionPattern, File outputDir) {
		this.projectRoot = projectRoot;
		this.inclusionPattern = inclusionPattern;
		this.outputDir = outputDir;
	}

	public static void main(String... args) {
		String projectRoot = args[0];
		String inclusionPattern = args[1];
		inclusionPattern = inclusionPattern.replace("/", File.separator);
		String output = args[2];
		new DocsFromSources(new File(projectRoot), inclusionPattern, new File(output)).generate();
	}

	public void generate() {
		Path path = this.projectRoot.toPath();
		Pattern pattern = Pattern.compile(this.inclusionPattern);
		logger.info("Inclusion pattern is [" + this.inclusionPattern + "]");
		List<TagKey> tagKeys = new ArrayList<>();
		FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!pattern.matcher(file.toString()).matches()) {
					logger.trace("File [" + file.toString() + "] doesn't match the inclusion pattern");
					return FileVisitResult.CONTINUE;
				}
				else if (!file.toString().endsWith(".java")) {
					logger.trace("Skipping [" + file.toString() + "] cause it's not java");
					return FileVisitResult.CONTINUE;
				}
				JavaUnit unit = Roaster.parseUnit(Files.newInputStream(file));
				JavaType myClass = unit.getGoverningType();
				if (!(myClass instanceof JavaEnumImpl)) {
					logger.trace("Will skip [" + myClass.getCanonicalName() + "] cause it's not an enum");
					return FileVisitResult.CONTINUE;
				}
				JavaEnumImpl myEnum = (JavaEnumImpl) myClass;
				if (!myEnum.getInterfaces()
						.contains(org.springframework.cloud.sleuth.TagKey.class.getCanonicalName())) {
					logger.debug("Will skip [" + myClass.getCanonicalName() + "] cause this enum does not implement ["
							+ org.springframework.cloud.sleuth.TagKey.class.getCanonicalName() + "]");
					return FileVisitResult.CONTINUE;
				}
				logger.info("Checking [" + myEnum.getName() + "]");
				if (myEnum.getEnumConstants().size() == 0) {
					logger.debug("Will skip [" + myClass.getCanonicalName() + "] cause this enum is empty");
					return FileVisitResult.CONTINUE;
				}
				for (EnumConstantSource enumConstant : myEnum.getEnumConstants()) {
					String name = enumKeyValue(enumConstant);
					String description = enumConstant.getJavaDoc().getText();
					tagKeys.add(new TagKey(name, description));
				}
				logger.info("Found [" + myEnum.getEnumConstants().size() + "] tag key entries in this class");
				return FileVisitResult.CONTINUE;
			}
		};

		try {
			Files.walkFileTree(path, fv);
			Path output = new File(this.outputDir, "_tags.adoc").toPath();
			StringBuilder stringBuilder = new StringBuilder().append(ADOC_HEADER);
			logger.info("Found [" + tagKeys.size() + "] tag keys");
			Collections.sort(tagKeys);
			tagKeys.forEach(tag -> stringBuilder.append(tag.toString()).append("\n"));
			stringBuilder.append("|===");
			Files.write(output, stringBuilder.toString().getBytes());
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private String enumKeyValue(EnumConstantSource enumConstant) {
		List<MemberSource<EnumConstantSource.Body, ?>> members = enumConstant.getBody().getMembers();
		if (members.isEmpty()) {
			logger.warn("No method declarations in the enum.");
			return "";
		}
		Object internal = members.get(0).getInternal();
		if (!(internal instanceof MethodDeclaration)) {
			logger.warn("Can't read the member [" + internal.getClass() + "] as a method declaration.");
			return "";
		}
		MethodDeclaration methodDeclaration = (MethodDeclaration) internal;
		if (methodDeclaration.getBody().statements().isEmpty()) {
			logger.warn("No return statements found. Continuing...");
			return "";
		}
		Object statement = methodDeclaration.getBody().statements().get(0);
		if (!(statement instanceof ReturnStatement)) {
			logger.warn("Statement [" + statement.getClass() + "] is not a return statement.");
			return "";
		}
		ReturnStatement returnStatement = (ReturnStatement) statement;
		Expression expression = returnStatement.getExpression();
		if (!(expression instanceof StringLiteral)) {
			logger.warn("Statement [" + statement.getClass() + "] is not a string literal statement.");
			return "";
		}
		return ((StringLiteral) expression).getLiteralValue();
	}

}

class TagKey implements Comparable<TagKey> {

	final String name;

	final String description;

	TagKey(String name, String description) {
		this.name = name;
		this.description = description;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TagKey tag = (TagKey) o;
		return Objects.equals(name, tag.name) && Objects.equals(description, tag.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description);
	}

	@Override
	public int compareTo(TagKey o) {
		return name.compareTo(o.name);
	}

	@Override
	public String toString() {
		return "|" + name + "|" + description;
	}

}
