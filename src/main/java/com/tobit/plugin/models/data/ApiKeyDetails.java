package com.tobit.plugin.models.data;

import java.util.List;

public record ApiKeyDetails(String key, String createdBy, String creationTime, String description,
                            List<ApiKeyRole> roles) {

}