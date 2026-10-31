package com.vyn.fakemoney;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.math.BigInteger;

public class FakeMoneyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Money.load();

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("fakemoney", "money_hud"),
                (ctx, tick) -> renderHud(ctx));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // /ui  -> toggle the menu
            dispatcher.register(ClientCommandManager.literal("ui").executes(c -> {
                Money.uiVisible = !Money.uiVisible;
                c.getSource().sendFeedback(Text.literal("Money UI " + (Money.uiVisible ? "shown" : "hidden"))
                        .formatted(Formatting.GRAY));
                return 1;
            }));

            // /setmoney <amount>  (1k 1m 1b 1t 1q 2.5m ...)
            dispatcher.register(ClientCommandManager.literal("setmoney")
                    .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                            .executes(c -> {
                                String raw = StringArgumentType.getString(c, "amount");
                                BigInteger v = Money.parse(raw);
                                if (v == null) {
                                    c.getSource().sendError(Text.literal(
                                            "Invalid amount. Examples: 500, 1k, 1m, 2.5b, 1t, 1q"));
                                    return 0;
                                }
                                Money.balance = v;
                                Money.save();
                                c.getSource().sendFeedback(Text.literal("Balance set to ")
                                        .formatted(Formatting.GRAY)
                                        .append(Text.literal("$").formatted(Formatting.GREEN))
                                        .append(Text.literal(" " + Money.abbreviate(v)).formatted(Formatting.WHITE)));
                                return 1;
                            })));

            // /paytoggle
            dispatcher.register(ClientCommandManager.literal("paytoggle").executes(c -> {
                Money.payEnabled = !Money.payEnabled;
                Money.save();
                c.getSource().sendFeedback(Text.literal("Payments are now ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(Money.payEnabled ? "ENABLED" : "DISABLED")
                                .formatted(Money.payEnabled ? Formatting.GREEN : Formatting.RED)));
                return 1;
            }));

            // /pay <name> <amount>   (name can be anything)
            dispatcher.register(ClientCommandManager.literal("pay")
                    .then(ClientCommandManager.argument("player", StringArgumentType.word())
                            .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                    .executes(c -> {
                                        FabricClientCommandSource src = c.getSource();
                                        String name = StringArgumentType.getString(c, "player");
                                        String raw = StringArgumentType.getString(c, "amount");

                                        if (!Money.payEnabled) {
                                            src.sendError(Text.literal("Payments are disabled. Use /paytoggle to enable."));
                                            return 0;
                                        }
                                        BigInteger v = Money.parse(raw);
                                        if (v == null || v.signum() <= 0) {
                                            src.sendError(Text.literal("Invalid amount. Examples: 500, 1k, 1m, 1b"));
                                            return 0;
                                        }
                                        if (v.compareTo(Money.balance) > 0) {
                                            src.sendError(Text.literal("You don't have enough money."));
                                            return 0;
                                        }

                                        Money.balance = Money.balance.subtract(v);
                                        Money.save();

                                        src.sendFeedback(Text.literal("You paid ")
                                                .formatted(Formatting.WHITE)
                                                .append(Text.literal(v.toString()).formatted(Formatting.WHITE))
                                                .append(Text.literal(" $").formatted(Formatting.GREEN))
                                                .append(Text.literal(" " + name).formatted(Formatting.WHITE)));

                                        // ding
                                        MinecraftClient.getInstance().getSoundManager().play(
                                                PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.5f, 1.0f));
                                        return 1;
                                    }))));
        });
    }

    private void renderHud(DrawContext ctx) {
        if (!Money.uiVisible) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        TextRenderer tr = mc.textRenderer;
        String name = mc.getSession().getUsername();
        String amount = Money.abbreviate(Money.balance);
        String dollar = "$ ";

        int textW = Math.max(tr.getWidth(name), tr.getWidth(dollar + amount));
        int boxW = textW + 14;
        int boxH = 32;
        int x = ctx.getScaledWindowWidth() - boxW - 6;
        int y = ctx.getScaledWindowHeight() / 2 - boxH / 2;

        ctx.fill(x, y, x + boxW, y + boxH, 0xAA000000);
        ctx.fill(x, y, x + 2, y + boxH, 0xFF55FF55);

        ctx.drawTextWithShadow(tr, name, x + 8, y + 6, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, "$", x + 8, y + 18, 0xFF55FF55);
        ctx.drawTextWithShadow(tr, amount, x + 8 + tr.getWidth(dollar), y + 18, 0xFFFFFFFF);
    }
}
