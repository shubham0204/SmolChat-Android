/*
 * Copyright (C) 2024 Shubham Panchal
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

package io.shubham0204.smollmandroid.prism4j

import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.Prism4j

class PrismGrammarLocator : GrammarLocator {
    override fun grammar(prism4j: Prism4j, language: String): Prism4j.Grammar? {
        return when (language) {
            "brainfuck" -> Prism_brainfuck.create(prism4j)
            "c" -> Prism_c.create(prism4j)
            "clike" -> Prism_clike.create(prism4j)
            "clojure" -> Prism_clojure.create(prism4j)
            "cpp" -> Prism_cpp.create(prism4j)
            "csharp" -> Prism_csharp.create(prism4j)
            "css" -> Prism_css.create(prism4j)
            "css_extras" -> Prism_css_extras.create(prism4j)
            "dart" -> Prism_dart.create(prism4j)
            "git" -> Prism_git.create(prism4j)
            "go" -> Prism_go.create(prism4j)
            "groovy" -> Prism_groovy.create(prism4j)
            "java" -> Prism_java.create(prism4j)
            "javascript" -> Prism_javascript.create(prism4j)
            "json" -> Prism_json.create(prism4j)
            "kotlin" -> Prism_kotlin.create(prism4j)
            "latex" -> Prism_latex.create(prism4j)
            "makefile" -> Prism_makefile.create(prism4j)
            "markdown" -> Prism_markdown.create(prism4j)
            "markup" -> Prism_markup.create(prism4j)
            "python" -> Prism_python.create(prism4j)
            "scala" -> Prism_scala.create(prism4j)
            "sql" -> Prism_sql.create(prism4j)
            "swift" -> Prism_swift.create(prism4j)
            "yaml" -> Prism_yaml.create(prism4j)
            else -> return null
        }
    }

    override fun languages(): MutableSet<String> =
        mutableSetOf(
            "brainfuck",
            "c",
            "clike",
            "clojure",
            "cpp",
            "csharp",
            "css",
            "css",
            "dart",
            "git",
            "go",
            "groovy",
            "java",
            "javascript",
            "json",
            "kotlin",
            "latex",
            "makefile",
            "markdown",
            "markup",
            "python",
            "scala",
            "sql",
            "swift",
            "yaml",
        )
}
