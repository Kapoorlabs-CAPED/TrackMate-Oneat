package fiji.plugin.trackmate.oneat;

import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readStringAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

@Plugin( type = TrackCorrectorFactory.class )
public class OneatCorrectorFactory implements TrackCorrectorFactory {

	
	public static final String DivisionFile = "Division_File";
	private static final String DEFAULT_DivisionFile = null;
	
	private String errorMessage;
	@Override
	public String getInfoText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImageIcon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TrackCorrector create(SpotCollection spots, DivisionSpotCollection specialspots, Settings settings, Model model,
			Map<String, Object> mapsettings) {
		
		
		  File oneatfile = (File) mapsettings.get(DivisionFile);
		
		  return new OneatCorrector(oneatfile, settings, model);
	}

	@Override
	public ConfigurationPanel getTrackCorrectorConfigurationPanel(Settings settings, Model model) {
		
		return new TrackCorrectorConfigPanel(settings, model);
	}

	@Override
	public boolean marshall(Map<String, Object> settings, Element element) {
		boolean ok = true;
		final StringBuilder str = new StringBuilder();
		ok = ok & writeAttribute( settings, element, DivisionFile, Double.class, str );
		return ok;
	}

	@Override
	public boolean unmarshall(Element element, Map<String, Object> settings) {
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readStringAttribute( element, settings, DivisionFile, errorHolder );
		return ok;
	}

	@Override
	public String toString( final Map< String, Object > settings )
	{
		if ( !checkSettingsValidity( settings ) ) { return errorMessage; }

		final String oneatfile = ( String ) settings.get( DivisionFile );
		final StringBuilder str = new StringBuilder();

		str.append( String.format( "  - oneat detection file: %.1f\n", oneatfile));

		return str.toString();
	}

	@Override
	public Map<String, Object> getDefaultSettings() {
		final Map< String, Object > sm = new HashMap<>( 1 );
		sm.put( DivisionFile, DEFAULT_DivisionFile );
		
		return sm;
	}

	@Override
	public boolean checkSettingsValidity(Map<String, Object> settings) {
		if ( null == settings )
		{
			errorMessage = "Settings map is null.\n";
			return false;
		}

		boolean ok = true;
		final StringBuilder str = new StringBuilder();

		ok = ok & checkParameter( settings, DivisionFile, String.class, str );

		if ( !ok )
		{
			errorMessage = str.toString();
		}
		return ok;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public OneatCorrectorFactory copy() {
		return new OneatCorrectorFactory();
	}

	

}
