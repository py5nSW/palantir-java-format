/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.palantir.javaformat.java;

import static com.google.common.truth.Truth.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Tests for command-line flags. */
@Execution(ExecutionMode.CONCURRENT)
public class CommandLineFlagsTest {

    // TODO(eaftan): Disallow passing both -lines and -offset/-length, like clang-format.

    @Test
    public void formatInPlaceRequiresAtLeastOneFile() throws UsageException {
        assertThatThrownBy(() -> Main.processArgs("-i")).isInstanceOf(UsageException.class);

        assertThatThrownBy(() -> Main.processArgs("-i", "-")).isInstanceOf(UsageException.class);

        Main.processArgs("-i", "Foo.java");
        Main.processArgs("-i", "Foo.java", "Bar.java");
    }

    @Test
    public void formatASubsetRequiresExactlyOneFile() throws UsageException {
        Main.processArgs("-lines", "10", "Foo.java");

        assertThatThrownBy(() -> Main.processArgs("-lines", "10")).isInstanceOf(UsageException.class);

        assertThatThrownBy(() -> Main.processArgs("-lines", "10", "Foo.java", "Bar.java"))
                .isInstanceOf(UsageException.class);

        Main.processArgs("-offset", "10", "-length", "10", "Foo.java");

        assertThatThrownBy(() -> Main.processArgs("-offset", "10", "-length", "10"))
                .isInstanceOf(UsageException.class);

        assertThatThrownBy(() -> Main.processArgs("-offset", "10", "-length", "10", "Foo.java", "Bar.java"))
                .isInstanceOf(UsageException.class);
    }

    // TODO(eaftan): clang-format allows a single offset with no length, which means to format
    // up to the end of the file.  We should match that behavior.
    @Test
    public void numberOfOffsetsMustMatchNumberOfLengths() throws UsageException {
        Main.processArgs("-offset", "10", "-length", "20", "Foo.java");

        assertThatThrownBy(() -> Main.processArgs("-offset", "10", "-length", "20", "-offset", "50", "Foo.java"))
                .isInstanceOf(UsageException.class);

        assertThatThrownBy(() -> Main.processArgs("-offset", "10", "-length", "20", "-length", "50", "Foo.java"))
                .isInstanceOf(UsageException.class);
    }

    @Test
    public void noFilesToFormatRequiresEitherHelpOrVersion() throws UsageException {
        Main.processArgs("-version");

        Main.processArgs("-help");

        assertThatThrownBy(() -> Main.processArgs()).isInstanceOf(UsageException.class);

        assertThatThrownBy(() -> Main.processArgs("-aosp")).isInstanceOf(UsageException.class);
    }

    @Test
    public void stdinAndFiles() {
        try {
            Main.processArgs("-", "A.java");
            fail("fail");
        } catch (UsageException e) {
            assertThat(e).hasMessageThat().contains("cannot format from standard input and files simultaneously");
        }
    }

    @Test
    public void inPlaceStdin() {
        try {
            Main.processArgs("-i", "-");
            fail("fail");
        } catch (UsageException e) {
            assertThat(e).hasMessageThat().contains("in-place formatting was requested but no files were provided");
        }
    }

    @Test
    public void inPlaceDryRun() {
        try {
            Main.processArgs("-i", "-n", "A.java");
            fail("fail");
        } catch (UsageException e) {
            assertThat(e).hasMessageThat().contains("cannot use --dry-run and --in-place at the same time");
        }
    }

    @Test
    public void assumeFileNameOnlyWorksWithStdin() {
        try {
            Main.processArgs("--assume-filename=Foo.java", "Foo.java");
            fail("fail");
        } catch (UsageException e) {
            assertThat(e)
                    .hasMessageThat()
                    .contains("--assume-filename is only supported when formatting standard input");
        }
    }
}
