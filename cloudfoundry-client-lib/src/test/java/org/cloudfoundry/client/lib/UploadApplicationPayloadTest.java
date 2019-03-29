/*
 * Copyright 2009-2012 the original author or authors.
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
 */

package org.cloudfoundry.client.lib;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.archive.ZipApplicationArchive;
import org.cloudfoundry.client.lib.domain.UploadApplicationPayload;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

/**
 * Tests for {@link org.cloudfoundry.client.lib.domain.UploadApplicationPayload}.
 *
 * @author Phillip Webb
 */
public class UploadApplicationPayloadTest {

    @Test
    public void verifyUploadContent() throws Exception {
        File testFile = SampleProjects.springTravel();
        ZipFile zipFile = new ZipFile(testFile);
        try {
            ApplicationArchive archive = new ZipApplicationArchive(zipFile);
            UploadApplicationPayload payload = new UploadApplicationPayload(archive);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileCopyUtils.copy(payload.getInputStream(), bos);
            assertThat(payload.getArchive(), is(archive));
            assertTrue(payload.getNumEntries() > 0);
            assertTrue(payload.getTotalUncompressedSize() > 0);
            assertEquals(Files.size(testFile.toPath()), (long) bos.toByteArray().length);
        } finally {
            zipFile.close();
        }
    }

}
