package fiji.plugin.trackmate;

import java.io.IOException;

import javax.swing.JFrame;

import fiji.plugin.trackmate.oneat.OneatCorrector;
import fiji.plugin.trackmate.oneat.TrackCorrectorConfigPanel;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;

public class OneatAttempt {

	public static void main( final String[] args ) throws IOException, InterruptedException
	{

		JFrame frame = new JFrame("");



		TrackCorrectorConfigPanel panel = new TrackCorrectorConfigPanel(null, null);
        panel.setVisible(true);;
		frame.getContentPane().add(panel, "Center");
		frame.setSize(panel.getPreferredSize());
		
	}

}
