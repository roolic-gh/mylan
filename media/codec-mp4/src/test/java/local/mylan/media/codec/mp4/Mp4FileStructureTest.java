/*
 * Copyright 2025 Ruslan Kashapov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package local.mylan.media.codec.mp4;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class Mp4FileStructureTest {

    @Test
    @Disabled
    void fileStructure() throws IOException {
//        final var path = Path.of("/home/roolic/test/SampleVideo_1280x720_1mb.mp4");
        final var path = Path.of("/home/roolic/test/ok-ko1.mp4");
//        final var path = Path.of("/home/roolic/test/mp4_CT-KO-1R-01x01.mp4");
        final var structure = new Mp4FileStructure(path, 1024);
        structure.trace(System.out);
    }
}
