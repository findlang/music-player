import java.util.ArrayList;
import java.util.Collections;
import javafx.scene.media.Media;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import javafx.scene.media.EqualizerBand;
import javafx.util.Duration;
import javafx.scene.media.AudioSpectrumListener;

enum PlayMode {
    REPEAT, CYCLE, SINGLE
}

class AngusListener implements AudioSpectrumListener {

    public float[] magnitudes;

    public AngusListener() {
        magnitudes = new float[256];
    }

    public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
        this.magnitudes = magnitudes;
    }
}

class MusicController {

	private ArrayList<Song> queue;

	private boolean paused = true;
	private PlayMode currentMode;
	private boolean shuffle = false;

	private MediaPlayer mediaPlayer;
	private AngusListener asl;

	public static final double sampleInterval = (double)1/30;
	public static final int numberOfBands = 128;

	public MusicController () {
		// TODO setCurrentMode(PlayMode.REPEAT);
		queue = new ArrayList<Song>();
		asl = new AngusListener();
		currentMode = PlayMode.CYCLE;
	}

	/*
	 * SETTERS
	 */

	//QUEUE

	public void addToQueueEnd(Song song) {
		queue.add(song);
		Main.mainSceneController.updateQueueArea();
	}

	public void addToQueueEnd(HasSongs hasSongs) {
		for (Song song : hasSongs.getSongs()) addToQueueEnd(song); // ^
	}

	public void addToQueueNext(Song song) {
		if (queue.size() == 0) {
			queue.add(0, song);
		} else {
			queue.add(1, song);
		}
		Main.mainSceneController.updateQueueArea();
	}

	public void addToQueueNext(HasSongs hasSongs) {
		if (queue.size() == 0) {
			queue.addAll(0, hasSongs.getSongs());
		} else {
			queue.addAll(1, hasSongs.getSongs());
		}
		Main.mainSceneController.updateQueueArea();
	}

	public void skipCurrentSongAndPlay(Song song) {
		addToQueueNext(song); // BROKEN TODO skips first song
		if (queue.size() > 1)
			nextSong();
		setPaused(false);
	}

	public void skipCurrentSongAndPlay(HasSongs hasSongs) {
		addToQueueNext(hasSongs);
		if (queue.size() > 1)
			nextSong();
		setPaused(false);
	}

	public void onSongEnd() {
		System.out.println("!!! SONG END");
		if (currentMode == PlayMode.REPEAT) {
			seekTo(0);
		} else if (currentMode == PlayMode.CYCLE) {
			//TODO
			nextSong();
		} else {
			nextSong();
		}
	}

	public void nextSong() {
		if (queue.size() > 0) {
			queue.remove(0);
			Main.mainSceneController.updateQueueArea();
		}

		if (queue.size() > 0) { //checks there is a next song to play TODO
			if (shuffle) {
				setSong(queue.get((int)(Math.random() * queue.size())));
			} else {
				setSong(queue.get(0));
			}
		} else {
			if (currentMode == PlayMode.CYCLE)
				setSong();
			else
				setSong();
		}
	}

	public void clearQueue() {
		if (queue.size() > 0) {
			Song temp = queue.get(0);
			queue.clear();
			queue.add(temp);
		}
		Main.mainSceneController.updateQueueArea();
	}

	//PAUSE / PLAY

	public void togglePaused() {
		setPaused(!isPaused());
	}

	public void setPaused(boolean p) {
		paused = p;
		if (paused) pause(); else play();
		Main.mainSceneController.updateQueueArea();
	}

	//PLAY MODE / SHUFFLE

	public void toggleShuffle() {
		shuffle = !shuffle;
	}

	public void cyclePlayMode() {
		switch (currentMode) {
			case REPEAT:
				setCurrentMode(PlayMode.CYCLE);
				break;
			case CYCLE:
				setCurrentMode(PlayMode.SINGLE);
				break;
			case SINGLE:
				setCurrentMode(PlayMode.REPEAT);
				break;
			default:
				break;
		}
	}

	private void setCurrentMode(PlayMode mode) {
		currentMode = mode;
		switch (currentMode) {
			case REPEAT:
				Main.mainSceneController.updatePlayModeButtonText("Repeat");
				break;
			case CYCLE:
				Main.mainSceneController.updatePlayModeButtonText("Cycle");
				break;
			case SINGLE:
				Main.mainSceneController.updatePlayModeButtonText("Single");
				break;
			default:
				break;
		}
	}

	public void seekTo(double pos) {
		//expect pos as a decimal 0 > 1 TODO
		mediaPlayer.seek(new Duration(pos));
	}

	/*
	 * GETTERS
	 */

	public boolean isPaused() { return paused; }

	public Song getCurrentSong() { return queue.size() > 0 ? queue.get(0) : null; }

	public String getTimeElapsed() { return mediaPlayer != null ? mediaPlayer.currentTimeProperty().toString() : ""; }

	public ArrayList<Song> getQueue() { return queue; }

	/*
	 * INTERNAL METHODS (only mention of mediaPlayer should be here)
	 */

	private void setSong(Song song) {
		File file = new File(song.getFile());
		assert file.exists() : "- NOT A REAL FILE";
		Media media = new Media(file.toURI().toString());

		if (mediaPlayer != null) mediaPlayer.stop();
		mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setOnEndOfMedia(() -> onSongEnd());
		mediaPlayer.setAudioSpectrumListener(asl);
		mediaPlayer.setAudioSpectrumInterval(sampleInterval);
		mediaPlayer.setAudioSpectrumNumBands(numberOfBands);
		setPaused(false);

        	Main.mainSceneController.updateQueueArea();
	}

	private void setSong() {
		setPaused(true);
	//	mediaPlayer.stop();
		mediaPlayer = null;
	}

	private void play() {
		if (mediaPlayer != null)
 			mediaPlayer.play();
		else if (queue.size() > 0)
			setSong(queue.get(0));
	}

	private void pause() {
		if (mediaPlayer != null)
 			mediaPlayer.pause();
	}

	/*
	 * NASTY VISUALISER STUFF
	 */

	public double getMagnitudeOfFrequency (int n) {
		if (isPaused()) return 0;

		double mag;
		//get ave
		if (n > 0) {
			mag = (asl.magnitudes[n-1]+asl.magnitudes[n]*3+asl.magnitudes[n+1]) / 5;
		} else {
			mag = asl.magnitudes[n];
		}

		mag = -mag;
		mag = 60 - mag;
		mag /= 60;

		// amplification of lesser used frequencies
		if (n > 3)
			return mag * 1.4;
		else if (n > 2)
			return mag * 1.2;
		else 
			return mag;
	}

}
