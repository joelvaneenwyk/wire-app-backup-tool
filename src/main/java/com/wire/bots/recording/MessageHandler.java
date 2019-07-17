package com.wire.bots.recording;

import com.wire.bots.recording.DAO.HistoryDAO;
import com.wire.bots.recording.model.DBRecord;
import com.wire.bots.recording.utils.Collector;
import com.wire.bots.recording.utils.Formatter;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class MessageHandler extends MessageHandlerBase {
    private static final String WELCOME_LABEL = "Recording was enabled.\nAvailable commands:\n" +
            "`/history` - receive previous messages\n" +
            "`/pdf` - receive previous messages in PDF format";
    private final HistoryDAO historyDAO;

    MessageHandler(HistoryDAO historyDAO) {
        this.historyDAO = historyDAO;
    }

    @Override
    public boolean onNewBot(NewBot newBot) {
        Logger.debug("onNewBot: bot: %s, user: %s", newBot.id, newBot.origin.id);
        return true;
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            client.sendText(WELCOME_LABEL);
        } catch (Exception e) {
            Logger.error("onNewConversation: %s %s", client.getId(), e);
        }
    }

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        UUID botId = UUID.fromString(client.getId());
        Logger.debug("onMemberJoin: %s users: %s", botId, userIds);

        try {
            Collector collector = collect(client, botId);

            for (String userId : userIds) {
                collector.sendPDF(UUID.fromString(userId));
            }
        } catch (Exception e) {
            Logger.error("onMemberJoin: %s %s", botId, e);
        }
    }

    @Override
    public void onBotRemoved(String botId) {
        Logger.debug("onBotRemoved: %s", botId);
        if (0 == historyDAO.unsubscribe(UUID.fromString(botId)))
            Logger.warning("Failed to unsubscribe. bot: %s", botId);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID userId = UUID.fromString(msg.getUserId());
        UUID botId = UUID.fromString(client.getId());
        String messageId = msg.getMessageId();

        Logger.debug("onText. bot: %s, msgId: %s", botId, messageId);
        try {
            String cmd = msg.getText().toLowerCase().trim();
            if (cmd.equals("/history")) {
                Formatter formatter = new Formatter();
                for (DBRecord record : historyDAO.getRecords(botId)) {
                    if (!formatter.add(record)) {
                        formatter.print(client, userId.toString());
                        formatter.add(record);
                    }
                }
                formatter.print(client, userId.toString());
                return;
            }

            if (cmd.equals("/pdf")) {
                client.sendDirectText("Generating PDF...", userId.toString());
                Collector collector = collect(client, botId);
                collector.sendPDF(userId);
                return;
            }

            if (cmd.equals("/html")) {
                client.sendDirectText("Generating HTML...", userId.toString());
                Collector collector = collect(client, botId);
                collector.sendHtml(userId);
                return;
            }

            Logger.debug("Inserting text, bot: %s %s", botId, messageId);

            User user = client.getUser(userId.toString());
            int timestamp = (int) (new Date().getTime() / 1000);
            if (0 == historyDAO.insertTextRecord(botId, messageId, user.name, msg.getText(), user.accent, userId, timestamp))
                Logger.warning("Failed to insert a text record. %s, %s", botId, messageId);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("OnText: %s ex: %s", client.getId(), e);
        }
    }

    @Override
    public void onEditText(WireClient client, TextMessage msg) {
        UUID botId = UUID.fromString(client.getId());
        String messageId = msg.getMessageId();
        if (0 == historyDAO.updateTextRecord(botId, messageId, msg.getText()))
            Logger.warning("Failed to update a text record. %s, %s", botId, messageId);
    }

    @Override
    public void onDelete(WireClient client, TextMessage msg) {
        UUID botId = UUID.fromString(client.getId());
        String messageId = msg.getMessageId();
        if (0 == historyDAO.remove(botId, messageId))
            Logger.warning("Failed to delete a record: %s, %s", botId, messageId);
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        String messageId = msg.getMessageId();
        UUID botId = UUID.fromString(client.getId());
        UUID userId = UUID.fromString(msg.getUserId());

        Logger.debug("onImage: %s type: %s, size: %,d KB, h: %d, w: %d, tag: %s",
                botId,
                msg.getMimeType(),
                msg.getSize() / 1024,
                msg.getHeight(),
                msg.getWidth(),
                msg.getTag()
        );

        try {
            User user = client.getUser(userId.toString());
            int timestamp = (int) (new Date().getTime() / 1000);

            int insertRecord = historyDAO.insertAssetRecord(botId,
                    messageId,
                    user.name,
                    msg.getMimeType(),
                    msg.getAssetKey(),
                    msg.getAssetToken(),
                    msg.getSha256(),
                    msg.getOtrKey(),
                    msg.getName(),
                    (int) msg.getSize(),
                    msg.getHeight(),
                    msg.getWidth(),
                    user.accent,
                    userId,
                    timestamp);

            if (0 == insertRecord)
                Logger.warning("Failed to insert image record. %s, %s", botId, messageId);
        } catch (Exception e) {
            Logger.error("onImage: %s %s %s", botId, messageId, e);
        }
    }

    @Override
    public void onVideoPreview(WireClient client, ImageMessage msg) {
        String messageId = msg.getMessageId();
        UUID botId = UUID.fromString(client.getId());
        UUID userId = UUID.fromString(msg.getUserId());

        Logger.debug("onVideoPreview: %s type: %s, size: %,d KB, h: %d, w: %d, tag: %s",
                botId,
                msg.getMimeType(),
                msg.getSize() / 1024,
                msg.getHeight(),
                msg.getWidth(),
                msg.getTag()
        );

        try {
            User user = client.getUser(msg.getUserId());
            int timestamp = (int) (new Date().getTime() / 1000);

            int insertRecord = historyDAO.insertAssetRecord(botId,
                    messageId,
                    user.name,
                    msg.getMimeType(),
                    msg.getAssetKey(),
                    msg.getAssetToken(),
                    msg.getSha256(),
                    msg.getOtrKey(),
                    msg.getName(),
                    (int) msg.getSize(),
                    msg.getHeight(),
                    msg.getWidth(),
                    user.accent,
                    userId,
                    timestamp);

            if (0 == insertRecord)
                Logger.warning("Failed to insert image record. %s, %s", botId, messageId);
        } catch (Exception e) {
            Logger.error("onVideoPreview: %s %s %s", botId, messageId, e);
        }
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage msg) {
        UUID botId = UUID.fromString(client.getId());
        String messageId = msg.getMessageId();
        UUID userId = UUID.fromString(msg.getUserId());

        Logger.debug("onAttachment: %s, name: %s, type: %s, size: %,d KB",
                botId,
                msg.getName(),
                msg.getMimeType(),
                msg.getSize() / 1024
        );

        try {
            User user = client.getUser(userId.toString());
            int timestamp = (int) (new Date().getTime() / 1000);
            int insertRecord = historyDAO.insertAssetRecord(botId,
                    messageId,
                    user.name,
                    msg.getMimeType(),
                    msg.getAssetKey(),
                    msg.getAssetToken(),
                    msg.getSha256(),
                    msg.getOtrKey(),
                    msg.getName(),
                    (int) msg.getSize(),
                    0,
                    0,
                    user.accent,
                    userId,
                    timestamp);

            if (0 == insertRecord)
                Logger.warning("Failed to insert attachment record. %s, %s", botId, messageId);
        } catch (Exception e) {
            Logger.error("onAttachment: %s %s %s", botId, messageId, e);
        }
    }

    private Collector collect(WireClient client, UUID botId) {
        Collector collector = new Collector(client);
        for (DBRecord record : historyDAO.getRecords(botId)) {
            try {
                collector.add(record);
            } catch (Exception e) {
                e.printStackTrace();
                Logger.warning("collect: %s", e);
            }
        }
        return collector;
    }
}
