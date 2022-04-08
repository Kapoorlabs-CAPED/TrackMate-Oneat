package fiji.plugin.trackmate.oneat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.util.TMUtils;
import net.imglib2.util.Util;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class TrackCorrectorRunner {

	private final static Context context = TMUtils.getContext();

	public static ArrayList<Integer> getTrackID(final Settings settings, final Model model, final ImgPlus<IntType> img,
			HashMap<Integer, ArrayList<Spot>> framespots, final boolean checkdivision) {

		ArrayList<Integer> TrackIDList = new ArrayList<Integer>();
		SpotCollection allspots = model.getSpots();

		
		int ndim = img.numDimensions();
		RandomAccess<IntType> ranac = img.randomAccess();
		for (Map.Entry<Integer, ArrayList<Spot>> framemap : framespots.entrySet()) {

			int frame = framemap.getKey();
			ArrayList<Spot> spotlist = framemap.getValue();

			for (Spot currentspots : spotlist) {

				long[] location = new long[ndim];
				for (int d = 0; d < ndim; ++d)
					location[d] = (long) currentspots.getDoublePosition(d);

				ranac.setPosition(frame, img.dimensionIndex(Axes.TIME));
				ranac.setPosition(location);
				// Get the label ID of the current interesting spot
				int labelID = ranac.get().get();

				// Now get the spot ID

				final Iterable<Spot> spotsIt = allspots.iterable(frame, false);
				for (final Spot spot : spotsIt) {

					// Now we have all the spots in this frame that are a part of the track

					long[] currentlocation = new long[ndim];
					for (int d = 0; d < ndim; ++d)
						currentlocation[d] = (long) spot.getDoublePosition(d);

					ranac.setPosition(currentlocation);
					
					int spotlabelID = ranac.get().get();
					
					if (spotlabelID == labelID) {
						
						int spotID = spot.ID();
						int trackID = model.getTrackModel().trackIDOf(spot);
						Boolean isDividing = isDividingTrack(spotID,trackID,model);
						if (checkdivision & isDividing == false)
						TrackIDList.add(trackID);
						if(checkdivision == false)
						TrackIDList.add(trackID);			
						
						
					}
				}

			}

		}

		return TrackIDList;
	}
	
	private static boolean isDividingTrack(final int spotID, final int trackID, final Model model) {
		
	   Boolean isDividing = false;
		
		
	   final Set<DefaultWeightedEdge> track = model.getTrackModel().trackEdges(trackID);
	  
	   for (final DefaultWeightedEdge e : track) {
           
			Spot Spotbase = model.getTrackModel().getEdgeSource(e);
			Spot Spottarget = model.getTrackModel().getEdgeTarget(e);
			int id = model.getTrackModel().trackIDOf(Spotbase);
			
			if (id == trackID) { 
		    int SpotbaseID = Spotbase.ID();
		    int SpottargeID = Spottarget.ID();
		    
		    ArrayList<Integer> TargetIDList = new ArrayList<Integer>();
		    if (SpotbaseID == spotID) 
		    	TargetIDList.add(SpottargeID);
		    //Source of two targets is a split point, no further check is needed	
		    if (TargetIDList.size() > 2) {
		    	
		    	isDividing = true;
		    	break;
		        	
		    }
			}
	   }
	   
	   return isDividing;
	}
	

	public static Pair<Pair<SpotCollection, HashMap<Integer, ArrayList<Spot>>>, Pair<SpotCollection, HashMap<Integer, ArrayList<Spot>>>> run(
			final Settings settings, final Model model, final File oneatdivisionfile, final File oneatapoptosisfile) {

		SpotCollection divisionspots = new SpotCollection();
		HashMap<Integer, ArrayList<Spot>> DivisionSpotListFrame = new HashMap<Integer, ArrayList<Spot>>();

		final LogService logService = context.getService(LogService.class);
		final StatusService statusService = context.getService(StatusService.class);
		final OptionsService optionService = context.getService(OptionsService.class);

		if (oneatdivisionfile != null) {
			String line = "";
			String cvsSplitBy = ",";
			int count = 0;

			ArrayList<Oneatobject> DivisionSpots = new ArrayList<Oneatobject>();
			HashMap<Integer, ArrayList<Oneatobject>> DivisionMap = new HashMap<Integer, ArrayList<Oneatobject>>();
			try (BufferedReader br = new BufferedReader(new FileReader(oneatdivisionfile))) {

				while ((line = br.readLine()) != null) {

					// use comma as separator
					String[] divisionspotsfile = line.split(cvsSplitBy);

					if (count > 0) {

						int time = Integer.parseInt(divisionspotsfile[0]);
						double Z = Double.parseDouble(divisionspotsfile[1]);
						double Y = Double.parseDouble(divisionspotsfile[2]);
						double X = Double.parseDouble(divisionspotsfile[3]);
						double score = Double.parseDouble(divisionspotsfile[4]);
						double size = Double.parseDouble(divisionspotsfile[5]);
						double confidence = Double.parseDouble(divisionspotsfile[6]);
						double angle = Double.parseDouble(divisionspotsfile[7]);

						RealPoint point = new RealPoint(X, Y, Z);
						Oneatobject Spot = new Oneatobject(time, point, score, size, confidence, angle);

						if (DivisionMap.get(time) == null) {
							DivisionSpots = new ArrayList<Oneatobject>();
							DivisionMap.put(time, DivisionSpots);
						} else
							DivisionMap.put(time, DivisionSpots);
						DivisionSpots.add(Spot);
					}
					count = count + 1;
				}
			}

			catch (IOException ie) {
				ie.printStackTrace();
			}

			// Parse each component.

			final Iterator<Entry<Integer, ArrayList<Oneatobject>>> iterator = DivisionMap.entrySet().iterator();

			while (iterator.hasNext()) {
				final Map.Entry<Integer, ArrayList<Oneatobject>> region = iterator.next();

				int frame = region.getKey();
				ArrayList<Oneatobject> currentcell = region.getValue();
				ArrayList<Spot> currentspots = new ArrayList<Spot>();
				for (Oneatobject cell : currentcell) {
					final double x = (cell.Location.getDoublePosition(0));
					final double y = (cell.Location.getDoublePosition(1));
					final double z = (cell.Location.getDoublePosition(2));

					double volume = cell.size;
					double quality = cell.size;
					int ndims = settings.imp.getNDimensions();

					final double radius = (ndims == 2) ? Math.sqrt(volume / Math.PI)
							: Math.pow(3. * volume / (4. * Math.PI), 1. / 3.);

					Spot currentspot = new Spot(x, y, z, radius, quality);
					currentspots.add(currentspot);
					divisionspots.add(currentspot, frame);
					DivisionSpotListFrame.put(frame, currentspots);
				}

			}

		}

		SpotCollection apoptosisspots = new SpotCollection();
		HashMap<Integer, ArrayList<Spot>> ApoptosisSpotListFrame = new HashMap<Integer, ArrayList<Spot>>();
		if (oneatapoptosisfile != null) {
			String line = "";
			String cvsSplitBy = ",";
			int count = 0;
			ArrayList<Oneatobject> ApoptosisSpots = new ArrayList<Oneatobject>();
			HashMap<Integer, ArrayList<Oneatobject>> ApoptosisMap = new HashMap<Integer, ArrayList<Oneatobject>>();
			try (BufferedReader br = new BufferedReader(new FileReader(oneatapoptosisfile))) {

				while ((line = br.readLine()) != null) {

					// use comma as separator
					String[] apoptosisspotsfile = line.split(cvsSplitBy);

					if (count > 0) {

						int time = Integer.parseInt(apoptosisspotsfile[0]);
						double Z = Double.parseDouble(apoptosisspotsfile[1]);
						double Y = Double.parseDouble(apoptosisspotsfile[2]);
						double X = Double.parseDouble(apoptosisspotsfile[3]);
						double score = Double.parseDouble(apoptosisspotsfile[4]);
						double size = Double.parseDouble(apoptosisspotsfile[5]);
						double confidence = Double.parseDouble(apoptosisspotsfile[6]);
						double angle = Double.parseDouble(apoptosisspotsfile[7]);

						RealPoint point = new RealPoint(X, Y, Z);
						Oneatobject Spot = new Oneatobject(time, point, score, size, confidence, angle);

						if (ApoptosisMap.get(time) == null) {
							ApoptosisSpots = new ArrayList<Oneatobject>();
							ApoptosisMap.put(time, ApoptosisSpots);
						} else
							ApoptosisMap.put(time, ApoptosisSpots);
						ApoptosisSpots.add(Spot);
					}
					count = count + 1;
				}
			}

			catch (IOException ie) {
				ie.printStackTrace();
			}

			// Parse each component.

			final Iterator<Entry<Integer, ArrayList<Oneatobject>>> iterator = ApoptosisMap.entrySet().iterator();

			while (iterator.hasNext()) {
				final Map.Entry<Integer, ArrayList<Oneatobject>> region = iterator.next();

				int frame = region.getKey();
				ArrayList<Oneatobject> currentcell = region.getValue();
				ArrayList<Spot> currentspots = new ArrayList<Spot>();
				for (Oneatobject cell : currentcell) {
					final double x = (cell.Location.getDoublePosition(0));
					final double y = (cell.Location.getDoublePosition(1));
					final double z = (cell.Location.getDoublePosition(2));

					double volume = cell.size;
					double quality = cell.size;
					int ndims = settings.imp.getNDimensions();

					final double radius = (ndims == 2) ? Math.sqrt(volume / Math.PI)
							: Math.pow(3. * volume / (4. * Math.PI), 1. / 3.);

					Spot currentspot = new Spot(x, y, z, radius, quality);
					currentspots.add(currentspot);
					apoptosisspots.add(currentspot, frame);
					ApoptosisSpotListFrame.put(frame, currentspots);
				}

			}

		}

		Pair<SpotCollection, HashMap<Integer, ArrayList<Spot>>> DivisionPair = new ValuePair<SpotCollection, HashMap<Integer, ArrayList<Spot>>>(
				divisionspots, DivisionSpotListFrame);

		Pair<SpotCollection, HashMap<Integer, ArrayList<Spot>>> ApoptosisPair = new ValuePair<SpotCollection, HashMap<Integer, ArrayList<Spot>>>(
				apoptosisspots, ApoptosisSpotListFrame);

		return new ValuePair<Pair<SpotCollection, HashMap<Integer, ArrayList<Spot>>>, Pair<SpotCollection, HashMap<Integer, ArrayList<Spot>>>>(
				DivisionPair, ApoptosisPair);
	}

}
