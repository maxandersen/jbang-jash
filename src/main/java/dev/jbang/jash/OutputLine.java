/*-
 *  § 
 * jash
 *    
 * Copyright (C) 2020 OnGres, Inc.
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
 * § §
 */

package dev.jbang.jash;

public class OutputLine {
	public final Integer fd;
	public final String line;

	OutputLine(Integer fd, String line) {
		this.fd = fd;
		this.line = line;
	}

	public Integer fd() {
		return fd;
	}

	public String line() {
		return line;
	}

	public boolean isStdout() {
		return fd == Jash.STDOUT;
	}

	public boolean isStderr() {
		return fd == Jash.STDERR;
	}

	@Override
	public String toString() {
		return fd + ":" + line;
	}
}
