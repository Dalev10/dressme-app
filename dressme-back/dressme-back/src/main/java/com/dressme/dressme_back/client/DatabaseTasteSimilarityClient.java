package com.dressme.dressme_back.client;

import com.dressme.dressme_back.schema.dto.TasteSimilarityBatchRequest;
import com.dressme.dressme_back.schema.dto.TasteSimilarityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "dressme-database-taste-client", url = "${app.services.database-url}")
public interface DatabaseTasteSimilarityClient {

    @PostMapping("/internal/taste-similarity/batch")
    List<TasteSimilarityResponse> computeBatch(@RequestBody TasteSimilarityBatchRequest request);
}
