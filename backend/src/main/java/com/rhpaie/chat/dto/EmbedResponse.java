package com.rhpaie.chat.dto;

import java.util.List;

public record EmbedResponse(List<List<Double>> vectors, String encoder) {
}
