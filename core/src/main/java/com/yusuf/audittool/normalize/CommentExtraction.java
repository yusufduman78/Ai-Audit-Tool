package com.yusuf.audittool.normalize;

import java.util.Set;

import com.yusuf.audittool.model.CommentContext;

public record CommentExtraction(CommentContext commentContext, Set<String> excludedPathPrefixes) {
}
