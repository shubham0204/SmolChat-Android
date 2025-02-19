/*
 * Copyright (C) 2025 Shubham Panchal
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

package io.shubham0204.smollmandroid.llm

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class PDFProcessor {
    suspend fun readPDF(pdfFileInputStream: InputStream): String = withContext(Dispatchers.Default) {
        val pdfReader = PdfReader(pdfFileInputStream)
        var text = ""
        for (i in 0..pdfReader.numberOfPages) {
            text += "\n\n" + PdfTextExtractor.getTextFromPage(pdfReader, i)
        }
        return@withContext text
    }
}
