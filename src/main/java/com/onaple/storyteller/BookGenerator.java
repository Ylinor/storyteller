package com.onaple.storyteller;

import com.flowpowered.math.vector.Vector3d;
import com.onaple.epicboundaries.EpicBoundaries;
import com.onaple.storyteller.action.*;
import com.onaple.storyteller.data.ActionEnum;
import com.onaple.storyteller.data.beans.ActionBean;
import com.onaple.storyteller.data.beans.ButtonBean;
import com.onaple.storyteller.data.beans.DialogBean;
import com.onaple.storyteller.data.beans.PageBean;

import org.apache.commons.lang3.EnumUtils;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class BookGenerator {
    @Inject
    private Game game;
    @Inject
    private DialogAction dialogAction;
    @Inject
    private ObjectiveAction objectiveAction;
    @Inject
    private KillCountAction killCountAction;
    @Inject
    private MiscellaneousAction miscellaneousAction;
    @Inject
    private InstanceAction instanceAction;

    public boolean displayBook (Player player,String entityName) {
        Optional<DialogBean> dialog = dialogAction.getDialogByTrigger(entityName, player);
        if (dialog.isPresent()) {
            BookView bookView = generateDialog(dialog.get());
            player.sendBookView(bookView);
        }
        return dialog.isPresent();
    }

    /**
     * Create a bookview from a dialog
     * @param dialog Dialog data
     * @return BookView to display
     */
    public BookView generateDialog(DialogBean dialog){
        BookView.Builder bookviewBuilder = BookView.builder();
        for (PageBean pageBean : dialog.getPages()) {
            bookviewBuilder.addPage(generatePage(pageBean, dialog.getTrigger()));
        }
        return bookviewBuilder.build();
    }

    public BookView generateDialog(String identifier) throws IllegalArgumentException {
        Optional<DialogBean> dialogOpt = dialogAction.getDialog(identifier);
        if(dialogOpt.isPresent()){
            return generateDialog(dialogOpt.get());
        }
        throw new IllegalArgumentException("Dialog Id not Found");
    }

    /**
     * Generate the text to print inside a page
     * @param page Page data
     * @return Text to display (with optional buttons)
     */
    private Text generatePage(PageBean page, List<String> npcNames){
        Text coloredContent = TextSerializers.FORMATTING_CODE.deserialize(page.getMessage());
        Text.Builder textBuilder = Text.builder();
        textBuilder.append(coloredContent);
        textBuilder.append(Text.of("\n"));
        if (!page.getButtonBeanList().isEmpty()) {
            List<ButtonBean> buttons = page.getButtonBeanList();
            for (ButtonBean buttonBean : buttons) {
                textBuilder.append(generateButton(buttonBean, npcNames));
            }
        }
        return textBuilder.build();
    }

    /**
     * Generate a button that will commit an action
     * @param buttonBean Button data
     * @return Printed button
     */
    private Text generateButton(ButtonBean buttonBean, List<String> npcNames) {
        Text coloredContent = TextSerializers.FORMATTING_CODE.deserialize(buttonBean.getText());
        Text.Builder textBuilder = Text.builder();
        textBuilder.append(Text.of("\n"));
        textBuilder.append(coloredContent);
        // Deprecated color system
        if (buttonBean.getColor() != null) {
            Optional<TextColor> textColor = game.getRegistry().getType(TextColor.class,buttonBean.getColor().toUpperCase());
            if (textColor.isPresent()) {
                textBuilder.color(textColor.get());
            }
        }
              // Concatenate NPC names
        String npcNamesString = "";
        for (String npcName : npcNames) {
            npcNamesString += npcName;
        }
        final String npcNameStringFinal = npcNamesString;
        // Set button action
        textBuilder.onClick(TextActions.executeCallback(commandSource-> {
            for (ActionBean action : buttonBean.getActions()) {
                switch (action.getName()) {
                    case OPEN_DIALOG:
                        changeDialog((Player)commandSource,action.getArg());
                        break;
                    case EXECUTE_COMMAND:
                        miscellaneousAction.executeCommand((Player)commandSource,action.getArg());
                        break;
                    case TELEPORT:
                        miscellaneousAction.teleport((Player)commandSource, action.getArg());
                        break;
                    case GIVE_ITEM:
                        miscellaneousAction.giveItem((Player)commandSource, action.getArg());
                        break;
                    case REMOVE_ITEM:
                        miscellaneousAction.removeItem((Player)commandSource, action.getArg());
                        break;
                    case SET_OBJECTIVE:
                        objectiveAction.setObjective((Player)commandSource, action.getArg());
                        break;
                    case START_KILL_COUNT:
                        killCountAction.startKillCount((Player)commandSource, npcNameStringFinal, action.getArg());
                        break;
                    case STOP_KILL_COUNT:
                        killCountAction.stopKillCount((Player)commandSource, npcNameStringFinal, action.getArg());
                        break;
                    case CREATE_INSTANCE:
                        String[] createInstanceParameters = action.getArg().split(" ");
                        if (createInstanceParameters.length >= 4) {
                            String[] positionValues = Arrays.copyOfRange(createInstanceParameters, 1, createInstanceParameters.length);
                            convertStringArrayToVector3d(positionValues).ifPresent(position -> {
                                instanceAction.createInstance(((Player)commandSource).getName(), createInstanceParameters[0], position);
                            });
                        }
                        break;
                    case APPARATE:
                        String[] apparateParameters = action.getArg().split(" ");
                        if (apparateParameters.length >= 4) {
                            String[] positionValues = Arrays.copyOfRange(apparateParameters, 1, apparateParameters.length);
                            convertStringArrayToVector3d(positionValues).ifPresent(position -> {
                                instanceAction.apparatePlayer(((Player)commandSource).getName(), apparateParameters[0], position);
                            });
                        }
                        break;
                }
            }
        }));
        return textBuilder.build();
    }

    /**
     * Convert a string[3] array to a Vector3d
     * @param array Array to convert
     * @return Optional of Vector3d if possible
     */
    private Optional<Vector3d> convertStringArrayToVector3d(String[] array) {
        if (array.length >= 3) {
            try {
                double[] position = new double[3];
                position[0] = Double.valueOf(array[0]);
                position[1] = Double.valueOf(array[1]);
                position[2] = Double.valueOf(array[2]);
                Vector3d positionVector = new Vector3d(position[0], position[1], position[2]);
                return Optional.of(positionVector);
            } catch (NumberFormatException e) {
                EpicBoundaries.getLogger().warn("Invalid parameters provided for position");
            }
        }
        return Optional.empty();
    }

    /**
     * Change the current dialog to a given dialog
     * @param source Player to show dialog to
     * @param dialogIndex Index of the dialog to show
     */
    private void changeDialog(Player source, String dialogIndex) {
        Optional<DialogBean> dialogBeanOptional = dialogAction.getDialog(dialogIndex);
        if(dialogBeanOptional.isPresent()){
            source.sendBookView(generateDialog(dialogBeanOptional.get()));
        } else {
            source.sendMessage(Text.builder("The dialog at the index : "+ dialogIndex + " cannot be loaded.").color(TextColors.RED).build());
        }
    }
}
