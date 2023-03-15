package com.llamalad7.betterchat.gui;

import com.google.common.collect.Lists;
import com.llamalad7.betterchat.BetterChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static com.llamalad7.betterchat.utils.AnimationTools.clamp;

@SideOnly(Side.CLIENT)
public class GuiBetterChat extends GuiNewChat {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Minecraft mc;
    /**
     * A list of messages previously sent through the chat GUI
     */
    private final List<String> sentMessages = Lists.newArrayList();
    /**
     * Chat lines to be displayed in the chat box
     */
    private final List<ChatLine> chatLines = Lists.newArrayList();
    /**
     * List of the ChatLines currently drawn
     */
    private final List<ChatLine> drawnChatLines = Lists.newArrayList();
    private int scrollPos;
    private boolean isScrolled;
    public static float percentComplete = 0.0F;
    public static int newLines;
    public static long prevMillis = -1;
    public boolean configuring;

    public GuiBetterChat(Minecraft mcIn) {
        super(mcIn);
        this.mc = mcIn;
    }

    public static void updatePercentage(long diff) {
        if (percentComplete < 1) percentComplete += 0.004f * diff;
        percentComplete = clamp(percentComplete, 0, 1);
    }

    public void drawChat(int updateCounter) {
        if (configuring) return;

        if (prevMillis == -1) {
            prevMillis = System.currentTimeMillis();
            return;
        }

        long current = System.currentTimeMillis();
        long diff = current - prevMillis;
        prevMillis = current;
        updatePercentage(diff);
        float t = percentComplete;
        float percent = 1 - (--t) * t * t * t;
        percent = clamp(percent, 0, 1);

        if (this.mc.gameSettings.chatVisibility == EntityPlayer.EnumChatVisibility.HIDDEN) {
            return;
        }

        int lineCount = this.getLineCount();
        int drawnLines = this.drawnChatLines.size();
        float opacity = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;

        if (drawnLines <= 0) {
            return;
        }

        float chatScale = this.getChatScale();
        int widthCoefficient = MathHelper.ceil((float) this.getChatWidth() / chatScale);

        GlStateManager.pushMatrix();
        if (BetterChat.getSettings().smooth && !this.isScrolled) {
            GlStateManager.translate(2.0F + BetterChat.getSettings().xOffset, 8.0F + BetterChat.getSettings().yOffset + (9 - 9*percent)*chatScale, 0.0F);
        } else {
            GlStateManager.translate(2.0F + BetterChat.getSettings().xOffset, 8.0F + BetterChat.getSettings().yOffset, 0.0F);
        }

        GlStateManager.scale(chatScale, chatScale, 1.0F);
        int l = 0;
        int inverseCounter=0;

        //for (int i1 = 0; i1 + this.scrollPos < drawnLines && i1 < lineCount; i1++) {
        for (int i1 = 0; i1 + this.scrollPos < drawnLines && i1 < lineCount; i1++) {




            ChatLine chatline = this.drawnChatLines.get(i1 + this.scrollPos);

            if (chatline != null) {
                int counterDifference = updateCounter - chatline.getUpdatedCounter();

                if (counterDifference < 200 || this.getChatOpen()) {
                    double counterDiffScaled = (double) counterDifference / 200.0D;
                    counterDiffScaled = 1.0D - counterDiffScaled;
                    counterDiffScaled = counterDiffScaled * 10.0D;
                    counterDiffScaled = MathHelper.clamp(counterDiffScaled, 0.0D, 1.0D);
                    counterDiffScaled = counterDiffScaled * counterDiffScaled;
                    int l1 = (int) (255.0D * counterDiffScaled);

                    if (this.getChatOpen()) {
                        l1 = 255;
                    }

                    l1 = (int) ((float) l1 * opacity);
                    ++l;

                    if (l1 > 3) {
                        inverseCounter++;
                        int i2 = 0;
                        int j2 = -i1 * 9;
                        if(BetterChat.getSettings().inverse){
                            j2=-j2;
                        }
                        if (!BetterChat.getSettings().clear) {
                            drawRect(-2, j2 - 9, i2 + widthCoefficient + 4, j2, l1 / 2 << 24);
                        }
                        String message = chatline.getChatComponent().getFormattedText();
                        GlStateManager.enableBlend();
                        if (BetterChat.getSettings().smooth && i1 <= newLines) {
                            this.mc.fontRenderer.drawStringWithShadow(message, 0.0F, (j2 - 8), 16777215 + ((int) (l1 * percent) << 24));
                        } else {
                            this.mc.fontRenderer.drawStringWithShadow(message, (float) i2, (float) (j2 - 8), 16777215 + (l1 << 24));
                        }
                        GlStateManager.disableAlpha();
                        GlStateManager.disableBlend();
                    }
                }
            }
        }

        if (this.getChatOpen()) {
            int fontHeight = this.mc.fontRenderer.FONT_HEIGHT;
            GlStateManager.translate(-3.0F, 0.0F, 0.0F);
            int l2 = drawnLines * fontHeight + drawnLines;
            int i3 = l * fontHeight + l;
            int j3 = this.scrollPos * i3 / drawnLines;
            int k1 = i3 * i3 / l2;

            if (l2 != i3) {
                int k3 = j3 > 0 ? 170 : 96;
                int l3 = this.isScrolled ? 13382451 : 3355562;
                drawRect(0, -j3, 2, -j3 - k1, l3 + (k3 << 24));
                drawRect(2, -j3, 1, -j3 - k1, 13421772 + (k3 << 24));
            }
        }

        GlStateManager.popMatrix();
    }

    /**
     * Clears the chat.
     */
    public void clearChatMessages(boolean p_146231_1_) {
        this.drawnChatLines.clear();
        this.chatLines.clear();

        if (p_146231_1_) {
            this.sentMessages.clear();
        }
    }

    public void printChatMessage(ITextComponent chatComponent) {
        this.printChatMessageWithOptionalDeletion(chatComponent, 0);
    }

    /**
     * prints the ChatComponent to Chat. If the ID is not 0, deletes an existing Chat Line of that ID from the GUI
     */
    public void printChatMessageWithOptionalDeletion(ITextComponent chatComponent, int chatLineId) {
        percentComplete = 0.0F;
        this.setChatLine(chatComponent, chatLineId, this.mc.ingameGUI.getUpdateCounter(), false);
        LOGGER.info("[CHAT] {}", chatComponent.getUnformattedText().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n"));
    }

    private void setChatLine(ITextComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly) {
        if (chatLineId != 0) {
            this.deleteChatLine(chatLineId);
        }

        int i = MathHelper.floor((float) this.getChatWidth() / this.getChatScale());
        List<ITextComponent> list = GuiUtilRenderComponents.splitText(chatComponent, i, this.mc.fontRenderer, false, false);
        boolean flag = this.getChatOpen();
        newLines = list.size() - 1;

        for (ITextComponent itextcomponent : list) {
            if (flag && this.scrollPos > 0) {
                this.isScrolled = true;
                this.scroll(1);
            }

            this.drawnChatLines.add(0, new ChatLine(updateCounter, itextcomponent, chatLineId));
        }

        while (this.drawnChatLines.size() > 100) {
            this.drawnChatLines.remove(this.drawnChatLines.size() - 1);
        }

        if (!displayOnly) {
            this.chatLines.add(0, new ChatLine(updateCounter, chatComponent, chatLineId));

            while (this.chatLines.size() > 100) {
                this.chatLines.remove(this.chatLines.size() - 1);
            }
        }
    }

    public void refreshChat() {
        this.drawnChatLines.clear();
        this.resetScroll();

        for (int i = this.chatLines.size() - 1; i >= 0; --i) {
            ChatLine chatline = this.chatLines.get(i);
            this.setChatLine(chatline.getChatComponent(), chatline.getChatLineID(), chatline.getUpdatedCounter(), true);
        }
    }

    /**
     * Gets the list of messages previously sent through the chat GUI
     */
    public List<String> getSentMessages() {
        return this.sentMessages;
    }

    /**
     * Adds this string to the list of sent messages, for recall using the up/down arrow keys
     */
    public void addToSentMessages(String message) {
        if (this.sentMessages.isEmpty() || !this.sentMessages.get(this.sentMessages.size() - 1).equals(message)) {
            this.sentMessages.add(message);
        }
    }

    /**
     * Resets the chat scroll (executed when the GUI is closed, among others)
     */
    public void resetScroll() {
        this.scrollPos = 0;
        this.isScrolled = false;
    }

    /**
     * Scrolls the chat by the given number of lines.
     */
    public void scroll(int amount) {
        this.scrollPos += amount;
        int i = this.drawnChatLines.size();

        if (this.scrollPos > i - this.getLineCount()) {
            this.scrollPos = i - this.getLineCount();
        }

        if (this.scrollPos <= 0) {
            this.scrollPos = 0;
            this.isScrolled = false;
        }
    }

    /**
     * Gets the chat component under the mouse
     */
    @Nullable
    public ITextComponent getChatComponent(int mouseX, int mouseY) {
        if (!this.getChatOpen()) {
            return null;
        } else {
            ScaledResolution scaledresolution = new ScaledResolution(this.mc);
            int i = scaledresolution.getScaleFactor();
            float f = this.getChatScale();
            int j = mouseX / i - 2 - BetterChat.getSettings().xOffset;
            int k = mouseY / i - 40 + BetterChat.getSettings().yOffset;
            j = MathHelper.floor((float) j / f);
            k = MathHelper.floor((float) k / f);

            if (j >= 0 && k >= 0) {
                int l = Math.min(this.getLineCount(), this.drawnChatLines.size());

                if (j <= MathHelper.floor((float) this.getChatWidth() / this.getChatScale()) && k < this.mc.fontRenderer.FONT_HEIGHT * l + l) {
                    int i1 = k / this.mc.fontRenderer.FONT_HEIGHT + this.scrollPos;

                    if (i1 >= 0 && i1 < this.drawnChatLines.size()) {
                        ChatLine chatline = this.drawnChatLines.get(i1);
                        int j1 = 0;

                        for (ITextComponent itextcomponent : chatline.getChatComponent()) {
                            if (itextcomponent instanceof TextComponentString) {
                                j1 += this.mc.fontRenderer.getStringWidth(GuiUtilRenderComponents.removeTextColorsIfConfigured(((TextComponentString) itextcomponent).getText(), false));

                                if (j1 > j) {
                                    return itextcomponent;
                                }
                            }
                        }
                    }

                }
                return null;
            } else {
                return null;
            }
        }
    }

    /**
     * Returns true if the chat GUI is open
     */
    public boolean getChatOpen() {
        return this.mc.currentScreen instanceof GuiChat;
    }

    /**
     * finds and deletes a Chat line by ID
     */
    public void deleteChatLine(int id) {
        Iterator<ChatLine> iterator = this.drawnChatLines.iterator();

        while (iterator.hasNext()) {
            ChatLine chatline = iterator.next();

            if (chatline.getChatLineID() == id) {
                iterator.remove();
            }
        }

        iterator = this.chatLines.iterator();

        while (iterator.hasNext()) {
            ChatLine chatline1 = iterator.next();

            if (chatline1.getChatLineID() == id) {
                iterator.remove();
                break;
            }
        }
    }

    public int getChatWidth() {
        return calculateChatboxWidth(this.mc.gameSettings.chatWidth);
    }

    public int getChatHeight() {
        return calculateChatboxHeight(this.getChatOpen() ? this.mc.gameSettings.chatHeightFocused : this.mc.gameSettings.chatHeightUnfocused);
    }

    /**
     * Returns the chatscale from mc.gameSettings.chatScale
     */
    public float getChatScale() {
        return this.mc.gameSettings.chatScale;
    }

    public static int calculateChatboxWidth(float scale) {
        int i = 320;
        int j = 40;
        return MathHelper.floor(scale * 280.0F + 40.0F);
    }

    public static int calculateChatboxHeight(float scale) {
        int i = 180;
        int j = 20;
        return MathHelper.floor(scale * 160.0F + 20.0F);
    }

    public int getLineCount() {
        return this.getChatHeight() / 9;
    }
}
