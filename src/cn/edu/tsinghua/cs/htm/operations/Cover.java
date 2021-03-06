package cn.edu.tsinghua.cs.htm.operations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import cn.edu.tsinghua.cs.htm.HTM;
import cn.edu.tsinghua.cs.htm.shapes.Convex;
import cn.edu.tsinghua.cs.htm.shapes.Trixel;
import cn.edu.tsinghua.cs.htm.utils.HTMid;
import cn.edu.tsinghua.cs.htm.utils.HTMRanges;
import cn.edu.tsinghua.cs.htm.utils.Markup;
import cn.edu.tsinghua.cs.htm.utils.Pair;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Cover a Convex with Trixels
 * @author Haojia Zuo
 *
 */
public class Cover {
	
	private Convex convex;
	
	private List<Trixel> listOfInners;
	
	private Stack<List<Trixel> > stackOfPartialLists;
	
	private Queue<Trixel> queue;
	
	private int maxLevel;
	
	private int currentLevel, previousLevel;
	
	private boolean halt, alreadyRun;
	
	public Cover(Convex convex, int maxLevel) {
		this.convex = convex;
		this.maxLevel = maxLevel;
		alreadyRun = false;
	}
	
	private void init() {
		listOfInners = new ArrayList<Trixel>();
		stackOfPartialLists = new Stack<List<Trixel> >();
		stackOfPartialLists.push(new ArrayList<Trixel>());
		queue = new LinkedList<Trixel>();
		currentLevel = 0;
		previousLevel = -1;
		
		HTM htm = HTM.getInstance();
		
		for (int i = 0; i < 8; i++) {
			queue.add(htm.getTopTrixel(i));
		}
		
		alreadyRun = false;
	}
	
	private void evaluateCurrentLevel() {
		if (currentLevel >= maxLevel) {
			halt = true;
		}
	}
	
	/**
	 * Calculate the Trixel cover
	 * Call this method before calling getTrixels(), getRanges(), etc.
	 */
	public void run() {
		init();
		halt = false;
		if (convex == null) {
			return;
		}
		while (!halt && queue.size() > 0) {
			step();
			evaluateCurrentLevel();
		}
		alreadyRun = true;
	}
	
	private void step() {
		boolean levelFinished = false;
		
		stackOfPartialLists.push(new ArrayList<Trixel>());
		currentLevel++;
		
		while (!levelFinished && queue.size() > 0) {
			Trixel t = queue.peek();
			if (t.getHTMid().getLevel() != previousLevel) {
				if (previousLevel < 0) {
					currentLevel = 0;
				} else {
					levelFinished = true;
				}
				previousLevel = t.getHTMid().getLevel();
			}
			if (!levelFinished) {
				queue.remove();
				boolean terminal = false;
				
				switch (t.getMarkup(convex)) {
				case Full:
					listOfInners.add(t);
					terminal = true;
					break;
				case Outside:
					terminal = true;
					break;
				case Partial:
					stackOfPartialLists.peek().add(t);
					break;
				default:
					terminal = true;
				}
				
				if (!terminal) {
					queue.addAll(t.expand());
				}
			}
		}
	}
	
	public List<Pair<HTMid, HTMid> > getHTMidPairs(int level) {
		if (!alreadyRun) {
			return null;
		}
		List<Trixel> trixelList = getTrixels();
		if (trixelList == null) {
			return null;
		}
		HTMRanges htmRanges = new HTMRanges(trixelList, level);
		return htmRanges.getPairList();
	}
	
	public List<Pair<HTMid, HTMid> > getHTMidPairs(Markup markup, int level) {
		if (!alreadyRun) {
			return null;
		}
		List<Trixel> trixelList = getTrixels(markup);
		if (trixelList == null) {
			return null;
		}
		HTMRanges htmRanges = new HTMRanges(trixelList, level);
		return htmRanges.getPairList();
	}
	
	/**
	 * Get all covering Trixels, no matter Full or Partial
	 * @return List of covering Trixels, null if failed
	 */
	public List<Trixel> getTrixels() {
		if (!alreadyRun) {
			return null;
		}
		return concatenate(listOfInners, stackOfPartialLists.peek());
	}
	
	/**
	 * Get specific kind of covering Trixels
	 * @param markUp Full or Partial
	 * @return List of covering Trixels, null if failed
	 */
	public List<Trixel> getTrixels(Markup markUp) {
		if (!alreadyRun) {
			return null;
		}
		switch (markUp) {
		case Full:
			return concatenate(listOfInners, null);
		case Partial:
			return concatenate(null, stackOfPartialLists.peek());
		default:
			return null;
		}
	}
	
	private List<Trixel> concatenate(List<Trixel> innerList, List<Trixel> partialList) {
		List<Trixel> result = new ArrayList<Trixel>();
		if (innerList != null) {
			result.addAll(innerList);
		}
		if (partialList != null) {
			result.addAll(partialList);
		}
		return result;
	}
	
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("l", false, "HTMid pairs in long int form");
		options.addOption("d", true, "maximum HTMid depth");
		options.addOption("latlon", false, "input points as latitude, longitude");
		options.addOption("file", true, "output file name");
		
		Option option = new Option("points", true,
				"vertices of query range in clockwise order");
		option.setRequired(true);
		option.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(option);
		
		CommandLineParser parser = new DefaultParser();
		
		try {
			CommandLine cmd = parser.parse(options, args);
			String[] vertices = cmd.getOptionValues("points");
			Convex convex = Convex.parseVertices(vertices, cmd.hasOption("latlon"));
			if (convex == null) {
				System.out.println("Illegal arguments!");
				return;
			}
			
			if (!cmd.hasOption("d")) {
				System.out.println("Must specify depth: -d [num]");
				return;
			}
			
			int depth = Integer.valueOf(cmd.getOptionValue("d"));
			
			Cover cover = new Cover(convex, depth);
			cover.run();
			List<Pair<HTMid, HTMid> > pairs = cover.getHTMidPairs(20);
			
			BufferedWriter bw = null;
			
			if (cmd.hasOption("file")) {
				String filename = cmd.getOptionValue("file");
				File file = new File(filename);
				if (!file.exists()) {
					file.createNewFile();
				}
				bw = new BufferedWriter(new FileWriter(file));
			}
			
			if (cmd.hasOption("l")) {
				for (Pair<HTMid, HTMid> pair : pairs) {
					if (cmd.hasOption("file")) {
						bw.write(pair.a.getId() + ", " +
										pair.b.getId());
						bw.newLine();
					} else {
						System.out.println(pair.a.getId() + ", " +
										pair.b.getId());
					}
				}
			} else {
				for (Pair<HTMid, HTMid> pair : pairs) {
					if (cmd.hasOption("file")) {
						bw.write(pair.a.toString() + ", " +
										pair.b.toString());
						bw.newLine();
					} else {
						System.out.println(pair.a.toString() + ", " +
										pair.b.toString());
					}
				}
			}
			
			if (cmd.hasOption("file")) {
				bw.flush();
				bw.close();
				System.out.println("ranges saved to " + cmd.getOptionValue("file"));
			}
			
		} catch (ParseException e) {
			System.out.println("Argument error!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("File output error!");
			e.printStackTrace();
		}
	}
	
}