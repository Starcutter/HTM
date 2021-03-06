package cn.edu.tsinghua.cs.htm;

import java.util.List;

import cn.edu.tsinghua.cs.htm.shapes.Cartesian;
import cn.edu.tsinghua.cs.htm.shapes.Trixel;
import cn.edu.tsinghua.cs.htm.utils.Constants;
import cn.edu.tsinghua.cs.htm.utils.HTMid;

/**
 * Top class for spherical HTM indexing.
 * @author Haojia Zuo
 *
 */
public class HTM {
	
	private static HTM instance = null;
	
	protected Cartesian[] origPoints;
	
	protected Trixel[] topTrixels;
	
	private HTM() {
		origPoints = new Cartesian[6];
		origPoints[0] = new Cartesian(0.0, 0.0, 1.0 * Constants.scale);
		origPoints[1] = new Cartesian(1.0 * Constants.scale, 0.0, 0.0);
		origPoints[2] = new Cartesian(0.0, 1.0 * Constants.scale, 0.0);
		origPoints[3] = new Cartesian(-1.0 * Constants.scale, 0.0, 0.0);
		origPoints[4] = new Cartesian(0.0, -1.0 * Constants.scale, 0.0);
		origPoints[5] = new Cartesian(0.0, 0.0, -1.0 * Constants.scale);
		
		topTrixels = new Trixel[8];
		topTrixels[0] = new Trixel(origPoints[1], origPoints[5], origPoints[2], new HTMid("S0"));
		topTrixels[1] = new Trixel(origPoints[2], origPoints[5], origPoints[3], new HTMid("S1"));
		topTrixels[2] = new Trixel(origPoints[3], origPoints[5], origPoints[4], new HTMid("S2"));
		topTrixels[3] = new Trixel(origPoints[4], origPoints[5], origPoints[1], new HTMid("S3"));
		topTrixels[4] = new Trixel(origPoints[1], origPoints[0], origPoints[4], new HTMid("N0"));
		topTrixels[5] = new Trixel(origPoints[4], origPoints[0], origPoints[3], new HTMid("N1"));
		topTrixels[6] = new Trixel(origPoints[3], origPoints[0], origPoints[2], new HTMid("N2"));
		topTrixels[7] = new Trixel(origPoints[2], origPoints[0], origPoints[1], new HTMid("N3"));
	}
	
	public static HTM getInstance() {
		if (instance == null) {
			instance = new HTM(); 
		}
		return instance;
	}
	
	public Trixel getTopTrixel(int i) {
		return topTrixels[i];
	}
	
	public HTMid Cartesian2HTMid(Cartesian p, int depth) {
		HTMid topID = getTopHTMid(p);
		if (topID.getId() < 8) {
			return new HTMid(0);
		}
		
		Trixel trixel = getTopTrixel((int)(topID.getId() - 8));
		while (depth-- > 0) {
			List<Trixel> children = trixel.expand();
			boolean found = false;
			for (Trixel child : children) {
				if (child.containsStrict(p)) {
					trixel = child;
					found = true;
					break;
				}
			}
			if (!found) {
				for (Trixel child : children) {
					if (child.containsLoose(p)) {
						trixel = child;
						found = true;
						break;
					}
				}
				if (!found) {
					return new HTMid(0);
				}
			}
		}
		
		return trixel.getHTMid();
	}
	
	public HTMid getTopHTMid(Cartesian p) {
		double[] xyz = p.get();
		double x = xyz[0];
		double y = xyz[1];
		double z = xyz[2];
		HTMid baseID;
		if (x > 0 && y >= 0) {
			baseID = (z >= 0) ? new HTMid("N3") : new HTMid("S0");
		} else if (x <= 0 && y > 0) {
			baseID = (z >= 0) ? new HTMid("N2") : new HTMid("S1");
		} else if (x < 0 && y <= 0) {
			baseID = (z >= 0) ? new HTMid("N1") : new HTMid("S2");
		} else if (x >= 0 && y < 0) {
			baseID = (z >= 0) ? new HTMid("N0") : new HTMid("S3");
		} else {
			baseID = (z >= 0) ? new HTMid("N3") : new HTMid("S0");
		}
		return baseID;
	}
	
}
