package com.radion.service.pipeline.parser;

import com.radion.domain.enums.Platform;
import com.radion.service.pipeline.models.NormalizedMessage;
import com.radion.service.pipeline.models.RawPayload;

public interface MessageParser {
    Platform getSupportedPlatform();
    NormalizedMessage parse(RawPayload payload);
}