/*-
 *  § 
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
 * § §
 */

package dev.jbang.jash;

import java.io.IOException;
import java.io.InputStream;

class ProcessOutputInputStream extends InputStream {
	private final Jash jash;
	private final InputStream inputStreamForOutput;
	private byte[] buffer = new byte[8192];
	private int bufferLength = 0;
	private int bufferIndex = 0;

	public ProcessOutputInputStream(Jash jash, InputStream inputStreamForOutput) {
		this.jash = jash;
		this.inputStreamForOutput = inputStreamForOutput;
		jash.registerCloseable(inputStreamForOutput);
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		if (!fill()) {
			return -1;
		}

		final int size = Math.min(len, this.bufferLength - this.bufferIndex);
		System.arraycopy(this.buffer, this.bufferIndex, buffer, off, size);
		this.bufferIndex += size;
		return size;
	}

	@Override
	public int read() throws IOException {
		if (!fill()) {
			return -1;
		}

		int read = buffer[bufferIndex++] & 0xFF;

		return read;
	}

	private boolean fill() throws IOException {
		if (isClosed()) {
			return false;
		}
		jash.checkTimeout();

		while (bufferIndex >= bufferLength) {
			final int length = Math.min(inputStreamForOutput.available(), buffer.length);
			if (length > 0) {
				bufferIndex = 0;
				bufferLength = inputStreamForOutput.read(buffer, 0, length);
			}
			if (bufferIndex >= bufferLength) {
				if (isClosed()) {
					return false;
				}
				jash.checkTimeout();

				jash.microSleep();
			}
		}
		return true;
	}

	@Override
	public int available() throws IOException {
		jash.checkTimeout();

		final int available = inputStreamForOutput.available();

		if (available <= 0) {
			jash.microSleep();
		}

		return available;
	}

	@Override
	public void close() throws IOException {
		jash.close();
	}

	public boolean isClosed() {
		try {
			return jash.isClosed()
					&& bufferIndex >= bufferLength
					&& inputStreamForOutput.available() <= 0;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
