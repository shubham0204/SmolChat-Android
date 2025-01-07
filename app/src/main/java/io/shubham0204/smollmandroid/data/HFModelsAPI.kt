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

package io.shubham0204.smollmandroid.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.shubham0204.hf_model_hub_api.HFModelInfo
import io.shubham0204.hf_model_hub_api.HFModelSearch
import io.shubham0204.hf_model_hub_api.HFModelTree
import io.shubham0204.hf_model_hub_api.HFModels
import org.koin.core.annotation.Single

@Single
class HFModelsAPI {
    suspend fun getModelInfo(modelId: String): HFModelInfo.ModelInfo = HFModels.getInfo().getModelInfo(modelId)

    suspend fun getModelTree(modelId: String): List<HFModelTree.HFModelFile> = HFModels.getTree().getModelFileTree(modelId)

    fun getModelsList(query: String) =
        Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = {
                HFModelSearchPagedDataSource(query)
            },
        ).flow

    class HFModelSearchPagedDataSource(
        private val query: String,
    ) : PagingSource<Int, HFModelSearch.ModelSearchResult>() {
        private val ggufModelFilter = "gguf,conversational"
        private val pageSize = 10

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HFModelSearch.ModelSearchResult> {
            val pageNumber = params.key ?: 1
            val result = HFModels.getSearch().searchModels(query, "", limit = pageSize, filter = ggufModelFilter)
            return LoadResult.Page(
                data = result,
                prevKey = null,
                nextKey = if (result.isEmpty()) null else pageNumber + 1,
            )
        }

        override fun getRefreshKey(state: PagingState<Int, HFModelSearch.ModelSearchResult>): Int? =
            state.anchorPosition?.let { anchorPosition ->
                state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
            }
    }
}
