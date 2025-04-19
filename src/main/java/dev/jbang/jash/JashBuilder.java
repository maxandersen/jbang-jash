/*-
 *  § 
 * jash
 *    
 * Copyright (C) 2019 - 2020 OnGres, Inc.
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * § §
 */

package dev.jbang.jash;

import static dev.jbang.jash.Jash.DEFAULT_EXIT_CODE_PREDICATE;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.jbang.jash.Jash.Shell;

public class JashBuilder {

	final String command;
	boolean asShell = false;
	Jash.Shell shell;
	String shellPrefix = Jash.DEFAULT_SHELLPREFIX;
	Supplier<CustomProcessBuilder<?>> processBuilderSupplier = () -> new JdkProcessBuilder(this);
	List<String> args = new ArrayList<>();
	Map<Integer, Integer> outputs = new HashMap<>(
			Stream	.of(
							Jash.STDOUT,
							Jash.STDERR)
					.collect(Collectors.toMap(fd -> fd, fd -> fd)));
	Path workPath = null;
	Map<String, String> environment = new HashMap<>(System.getenv());
	boolean closeAfterLast = true;
	Predicate<Integer> exitCodePredicate = DEFAULT_EXIT_CODE_PREDICATE;
	Duration timeout = null;

	/**
	 * Create a builder for specified command.
	 */
	public JashBuilder(String command) {
		this.command = command;
	}

	/**
	 * Overwrite command arguments.
	 */
	public JashBuilder args(List<String> args) {
		this.args.clear();
		this.args.addAll(args);
		return this;
	}

	/**
	 * Overwrite command arguments.
	 */
	public JashBuilder args(String... args) {
		args(Arrays.asList(args));
		return this;
	}

	/**
	 * Add a command argument.
	 */
	public JashBuilder arg(String arg) {
		this.args.add(arg);
		return this;
	}

	/**
	 * Add a multiline command argument.
	 */
	public JashBuilder multilineArg(String... lines) {
		return multilineArg(Arrays.asList(lines).stream());
	}

	/**
	 * Add a multiline command argument.
	 */
	public JashBuilder multilineArg(Collection<String> lines) {
		return multilineArg(lines.stream());
	}

	/**
	 * Add a multiline command argument.
	 */
	public JashBuilder multilineArg(Stream<String> lines) {
		this.args.add(lines.collect(Collectors.joining(Jash.NEWLINE_DELIMITER)));
		return this;
	}

	/**
	 * Set the command working path.
	 */
	public JashBuilder workPath(Path workPath) {
		this.workPath = workPath;
		return this;
	}

	/**
	 * Remove environment variables inherited by the JVM.
	 */
	public JashBuilder clearEnvironment() {
		this.environment.clear();
		return this;
	}

	/**
	 * Overwrite the command environment variables.
	 */
	public JashBuilder environment(Map<String, String> environment) {
		this.environment.putAll(environment);
		return this;
	}

	/**
	 * Set an environment variable for the command.
	 */
	public JashBuilder environment(String name, String value) {
		this.environment.put(name, value);
		return this;
	}

	/**
	 * When specified will cause the stream of a failed process to throw exception
	 * only when specifically closing it.
	 * <p>
	 * By default a failed process will throw an exception when beyond the last
	 * element.
	 * </p>
	 */
	public JashBuilder dontCloseAfterLast() {
		this.closeAfterLast = false;
		return this;
	}

	/**
	 * Collection of allowed exit code values that will be considered as successful
	 * exit codes for the command.
	 * <p>
	 * Warning: overrides the default value that considers 0 as a successful exit
	 * code.
	 * </p>
	 */
	public JashBuilder allowedExitCodes(Collection<Integer> exitCodes) {
		this.exitCodePredicate = exitCodes::contains;
		return this;
	}

	public JashBuilder allowedExitCodes(int... exitCodes) {
		this.exitCodePredicate = Arrays.stream(exitCodes).boxed().collect(Collectors.toList())::contains;
		return this;
	}

	/**
	 * Set a custom exit code predicate.
	 * <p>
	 * Warning: overrides the default value that considers 0 as a successful exit
	 * code.
	 * </p>
	 * 
	 * @param exitCodePredicate
	 * @return
	 */
	public JashBuilder withExitCodePredicate(Predicate<Integer> exitCodePredicate) {
		this.exitCodePredicate = exitCodePredicate;
		return this;
	}

	/**
	 * Add an allowed exit code that will be considered a successful exit code for
	 * the command.
	 */
	public JashBuilder allowedExitCode(int exitCode) {
		this.exitCodePredicate = code -> code == exitCode;
		return this;
	}

	/**
	 * Redirect stdout to stderr.
	 */
	public JashBuilder redirectStdoutToStderr() {
		this.outputs.put(Jash.STDOUT, Jash.STDERR);
		return this;
	}

	/**
	 * Redirect stderr to stdout.
	 */
	public JashBuilder redirectStderrToStdout() {
		this.outputs.put(Jash.STDERR, Jash.STDOUT);
		return this;
	}

	/**
	 * Do not output stdout.
	 */
	public JashBuilder noStdout() {
		this.outputs.remove(Jash.STDOUT);
		return this;
	}

	/**
	 * Do not output stderr.
	 */
	public JashBuilder noStderr() {
		this.outputs.remove(Jash.STDERR);
		return this;
	}

	/**
	 * Specifies the command that will be prefixed to all shell or pipeShell runs
	 */
	public JashBuilder withShellPrefix(String prefix) {
		this.shellPrefix = prefix;
		return this;
	}

	/**
	 * Specifies which shell and arguments to use for shell or pipeShell
	 * 
	 * Example: withShell("cmd.exe", "/C");
	 */
	public JashBuilder withShell(String shell, String shellArg) {
		this.shell = new Jash.Shell(shell, shellArg);
		return withShell();
	}

	/**
	 * Start the process (in background) and return a {@code Jash} instance wrapping
	 * the running process.
	 */
	public Jash start() {
		try {
			CustomProcessBuilder<?> builder = processBuilderSupplier.get();
			if (workPath != null) {
				builder.directory(workPath);
			}
			builder.setEnvironment(environment);
			CustomProcess process = builder.start();
			Instant start = Instant.now();
			return new Jash(this, builder, process, start);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public JashBuilder withShell() {
		asShell = true;
		return this;
	}

	Shell getShell() {
		return shell == null ? Jash.getDefaultShell() : shell;
	}

	public JashBuilder withAnyExitCode() {
		this.exitCodePredicate = (code) -> true;
		return this;
	}
}
