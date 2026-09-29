package com.rhpaie.chat.dto;

public record ChatToolResult(
        String facts,
        String templateReply,
        String link,
        String linkLabel,
        ChatProposedAction proposedAction
) {

    public static ChatToolResult reply(String templateReply) {
        return new ChatToolResult(templateReply, templateReply, null, null, null);
    }

    public static ChatToolResult reply(String facts, String templateReply, String link, String linkLabel) {
        return new ChatToolResult(facts, templateReply, link, linkLabel, null);
    }

    public static ChatToolResult propose(
            String facts,
            String templateReply,
            String link,
            String linkLabel,
            ChatProposedAction proposedAction
    ) {
        return new ChatToolResult(facts, templateReply, link, linkLabel, proposedAction);
    }
}
