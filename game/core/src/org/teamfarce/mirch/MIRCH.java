package org.teamfarce.mirch;

import com.badlogic.gdx.Game;
import org.teamfarce.mirch.ScenarioBuilder.ScenarioBuilderException;
import org.teamfarce.mirch.dialogue.Dialogue;
import org.teamfarce.mirch.entities.Suspect;
import org.teamfarce.mirch.map.Room;
import org.teamfarce.mirch.screens.JournalScreen;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

/**
 * MIRCH is used to generate all graphics in the program. It initialises the scenario generator and
 * game state and provides all interactions with the back end of the program.
 */
public class MIRCH extends Game {
    ArrayList<GameSnapshot> gameSnapshots = new ArrayList();
    public int currentSnapshot = 0;
    int PLAYERNO = 2;
    public GUIController guiController;

    /**
     * Initialises all variables in the game and sets up the game for play.
     */
    @Override
    public void create() {
        Assets.load();

        this.generate(2);
    }
    
    public void generate(int no){
    	this.PLAYERNO = no;
        long seed = System.currentTimeMillis();

    	ScenarioBuilderDatabase database;
        try {
            database = new ScenarioBuilderDatabase("db.db");

            try {
                for (int i = 0; i < PLAYERNO; i++) {
                    this.gameSnapshots
                        .add(ScenarioBuilder.generateGame(this, database, new Random(seed)));
                }
            } catch (ScenarioBuilderException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Setup screens
        this.guiController = new GUIController(this);
        this.guiController.initScreens();
    }
    
    /**
     * @author Team FARCE - Jacob Unwin
     * Trims the number of players from a maximum of 2
     * @param no
     */
    public void trimPlayers(int no){
    	this.PLAYERNO = no;
    	for (int i = no; i < this.gameSnapshots.size(); i++){
    		this.gameSnapshots.remove(i);
    	}
    }

    /**
     * The render function deals with all game logic. It receives inputs from the input controller,
     * carries out logic and pushes outputs to the screen through the GUIController
     */
    @Override
    public void render() {
        this.guiController.update();
        super.render();
    }

    @Override
    public void dispose() {

    }

    /**
     * Finds and returns the current game snapshot.
     *
     * @return The game snapshot object.
     */
    public GameSnapshot getCurrentGameSnapshot() {
        return this.gameSnapshots.get(this.currentSnapshot);
    }

    /**
     * Moves the game to the next game snapshot.
     */
    public void nextGameSnapshot() {
        this.currentSnapshot += 1; // increment the current snapshot pointer
        // move the snapshot pointer back to 0 if its reached the end of the list
        if (this.currentSnapshot >= this.gameSnapshots.size()) {
            this.currentSnapshot = 0;
        }

    }

    /**
     * Adds and sets the current snapshot to the one given.
     *
     * @param snapshot The snapshot to use.
     */
    public void setGameSnapshot(GameSnapshot snapshot) {
        this.gameSnapshots.add(snapshot);
        this.currentSnapshot = this.gameSnapshots.size() - 1;
    }
}
