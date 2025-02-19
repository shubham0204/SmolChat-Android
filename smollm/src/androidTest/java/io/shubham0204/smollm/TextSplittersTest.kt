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

package io.shubham0204.smollm

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextSplittersTest {
    private val text =
        """
        The old lighthouse stood sentinel against the crashing waves, its beam a solitary finger of light piercing the inky blackness. 
        For generations, it had warned sailors of the treacherous reefs lurking just beneath the surface, a silent guardian against the sea's unpredictable moods.  
        
        
        The wind howled a mournful song around its weathered tower, and the spray from the ocean lashed against its thick glass windows, yet the light continued to shine, a beacon of hope in the vast, 
        unforgiving expanse of the sea.  
        
        Inside, the keeper meticulously tended to the lamp, ensuring its unwavering glow, knowing that lives depended on the steady rhythm of its pulse.  
        He was a solitary figure, bound to his duty, his world confined to the rhythmic sweep of the light and the ceaseless roar of the ocean.
        """.trimIndent()

    @Test
    fun splitByWhiteSpace_works() =
        runTest {
            val chunks = TextSplitters.splitByWhiteSpace(text, chunkSize = 50)
            assert(chunks.isNotEmpty())
        }
}
