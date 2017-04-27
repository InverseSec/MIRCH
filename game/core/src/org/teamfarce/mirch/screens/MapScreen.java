package org.teamfarce.mirch.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ScreenUtils;
import org.teamfarce.mirch.MIRCH;
import org.teamfarce.mirch.OrthogonalTiledMapRendererWithPeople;
import org.teamfarce.mirch.entities.AbstractPerson;
import org.teamfarce.mirch.entities.PlayerController;
import org.teamfarce.mirch.entities.Suspect;
import org.teamfarce.mirch.screens.elements.RoomArrow;
import org.teamfarce.mirch.screens.elements.StatusBar;

import java.util.List;




/**
 * Created by brookehatton on 31/01/2017.
 */
public class MapScreen extends AbstractScreen {

    /**
     * This stores the most recent frame as an image
     */
    public static Image recentFrame;
    public static boolean grabScreenshot = false;
    /**
     * This is the list of NPCs who are in the current room
     */
    private OrthographicCamera camera;
    private PlayerController playerController;

    /**
     * This stores the room arrow that is drawn when the player stands on a room changing mat
     */
    private RoomArrow arrow = new RoomArrow(game.getCurrentGameSnapshot().player);
    /**
     * This is the sprite batch that is relative to the screens origin
     */
    private SpriteBatch spriteBatch;
    /**
     * This stores whether the room is currently in transition or not
     */
    private boolean roomTransition = false;
    /**
     * The amount of ticks it takes for the black to fade in and out
     */
    private float ANIM_TIME = 0.7f;

    /**
     * The black sprite that is used to fade in/out
     */
    private Sprite BLACK_BACKGROUND = new Sprite();
    /**
     * The current animation frame of the fading in/out
     */
    private float animTimer = 0.0f;
    /**
     * This boolean determines whether the black is fading in or out
     */
    private boolean fadeToBlack = true;
    private Music bgm;
    private StatusBar statusBar;

    public MapScreen(MIRCH game, Skin uiSkin) {
        super(game);
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, w, h);
        this.camera.update();
        this.playerController = new PlayerController(game, camera);
        this.spriteBatch = new SpriteBatch();
        this.bgm = Gdx.audio.newMusic(Gdx.files.internal("Minima.mp3"));
        this.bgm.setLooping(true);


        Pixmap pixMap =
            new Pixmap(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Pixmap.Format.RGBA8888);

        pixMap.setColor(Color.BLACK);
        pixMap.fill();

        BLACK_BACKGROUND = new Sprite(new Texture(pixMap));

        this.statusBar = new StatusBar(game, uiSkin);
    }

    private PlayerController getPlayerController() {
        return this.playerController;
    }

    private StatusBar getStatusBar() {
        return this.statusBar;
    }

    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(this.getStatusBar().stage);
        multiplexer.addProcessor(this.getPlayerController());
        Gdx.input.setInputProcessor(multiplexer);
        this.bgm.play();
    }

    @Override
    public void render(float delta) {
        final OrthogonalTiledMapRendererWithPeople tileRenderer = this.getTileRenderer();

        game.getCurrentGameSnapshot().scoreTracker.incrementGameTicks();
        this.getPlayerController().update(delta);
        game.getCurrentGameSnapshot().player.update(delta);

        // loop through each suspect character, moving them randomly
        for (Suspect character: this.getNPCs()) {
            character.update(delta);
        }

        camera.position.x = game.getCurrentGameSnapshot().player.getX();
        camera.position.y = game.getCurrentGameSnapshot().player.getY();
        camera.update();
        tileRenderer.setView(camera);

        tileRenderer.render();
        tileRenderer.getBatch().begin();
        arrow.update();
        arrow.draw(tileRenderer.getBatch());
        game.getCurrentGameSnapshot().player
            .getRoom()
            .drawClues(delta, tileRenderer.getBatch());

        tileRenderer.getBatch().end();

        updateTransition(delta);

        // Everything to be drawn relative to bottom left of the screen
        spriteBatch.begin();

        if (roomTransition) {
            BLACK_BACKGROUND.draw(spriteBatch);
        }

        spriteBatch.end();

        if (!grabScreenshot) {
            this.getStatusBar().render();
        }

        if (grabScreenshot) {
            recentFrame = new Image(ScreenUtils.getFrameBufferTexture());
        }
    }

    /**
     * This is called when the player decides to move to another room
     */
    public void initialiseRoomTransition() {
        game.getCurrentGameSnapshot().setAllUnlocked();
        roomTransition = true;
    }

    /**
     * This is called when the room transition animation has completed so the necessary variables
     * can be returned to their normal values
     */
    public void finishRoomTransition() {
        animTimer = 0;
        roomTransition = false;
        fadeToBlack = true;
    }

    /**
     * This method returns true if the game is currently transitioning between rooms
     */
    public boolean isTransitioning() {
        return roomTransition;
    }

    /**
     * This method is called once a render loop to update the room transition animation
     */
    private void updateTransition(float delta) {
        if (roomTransition) {
            BLACK_BACKGROUND.setAlpha(Interpolation.pow4.apply(0, 1, animTimer / ANIM_TIME));

            if (fadeToBlack) {
                animTimer += delta;

                if (animTimer >= ANIM_TIME) {
                    game.getCurrentGameSnapshot().player.moveRoom();
                    fadeToBlack = false;
                }
            } else {
                animTimer -= delta;

                if (animTimer <= 0f) {
                    finishRoomTransition();
                }
            }
        }

        if (game.getCurrentGameSnapshot().player.roomChange) {
            initialiseRoomTransition();
            game.getCurrentGameSnapshot().player.roomChange = false;
        }
    }

    /**
     * This method returns the NPCs on the current map
     *
     * @return The Suspects on the current map
     */
    public List<Suspect> getNPCs() {
        return game
            .getCurrentGameSnapshot()
            .map
            .getNPCs(game.getCurrentGameSnapshot().player.getRoom());
    }

    @Override
    public void resize(int width, int height) {
        this.getStatusBar().resize(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        this.getStatusBar().dispose();
        this.bgm.pause();
    }

    private OrthogonalTiledMapRendererWithPeople getTileRenderer() {
        OrthogonalTiledMapRendererWithPeople tileRender =
            new OrthogonalTiledMapRendererWithPeople(
                game.getCurrentGameSnapshot().player.getRoom().getTiledMap(),
                game
            );
        tileRender.addPerson(game.getCurrentGameSnapshot().player);
        tileRender.addPerson((List<AbstractPerson>) ((List<? extends AbstractPerson>) this.getNPCs()));
        return tileRender;
    }
}
