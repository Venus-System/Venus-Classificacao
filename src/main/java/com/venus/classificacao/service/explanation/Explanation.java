package com.venus.classificacao.service.explanation;

import java.util.List;

public record Explanation(List<Reason> reasons, String summary) {

    public Explanation {
        reasons = List.copyOf(reasons);
    }
}
