/**
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.neba.core.util;

import java.io.File;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * @author Olaf Otto
 */
public class ZipFileUtil {
    private static final Pattern WINDOWS_FILE_SEPARATOR = compile("[\\\\]+");
    private static final Pattern DUPLICATE_SEPARATORS = compile("[/]{2,}");

    public static String toZipFileEntryName(File file) {
        if (file == null) {
            throw new IllegalArgumentException("method parameter file must not be null");
        }

        // Strip any device location identifiers, e.g. 'd:' in windows systems
        String filePath = file.getAbsolutePath();

        String entryName = filePath.replace(':', '/');
        entryName = WINDOWS_FILE_SEPARATOR.matcher(entryName).replaceAll("/");
        entryName = DUPLICATE_SEPARATORS.matcher(entryName).replaceAll("/");

        while (entryName.charAt(0) == '/') {
            entryName = entryName.substring(1);
        }
        return entryName;
    }

    private ZipFileUtil() {
    }
}
