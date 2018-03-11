package cn.edu.tsinghua.cs.htm.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import cn.edu.tsinghua.cs.htm.shapes.Trixel;

/**
 * A list of HTMid ranges of the same depth
 * Used as the result of Trixel covering
 * @author Haojia Zuo
 *
 */
public class HTMRanges {
	
	private List<Pair<HTMid, HTMid> > pairList;
	
	/**
	 * All HTMids will be at this same level
	 */
	private int level;
	
	{
		pairList = new ArrayList<Pair<HTMid, HTMid> >();
	}
	
	public HTMRanges(int level) {
		this.level = level;
	}
	
	public HTMRanges(List<Pair<HTMid, HTMid> > pairList) {
		if (pairList != null) {
			this.pairList.addAll(pairList);
			sort();
			compact();
			level = this.pairList.get(0).a.getLevel();
		}
	}
	
	public HTMRanges(List<Trixel> trixelList, int level) {
		this.level = level;
		if (trixelList != null) {
			for (Trixel trixel : trixelList) {
				HTMid htmId = trixel.getHTMid();
				Pair<HTMid, HTMid> range = htmId.extend(this.level);
				pairList.add(range);
			}
			sort();
			compact();
		}
	}
	
	public static HTMRanges fromFile(String filename) throws IOException {
		File file = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		List<Pair<HTMid, HTMid> > pairList = new ArrayList<Pair<HTMid, HTMid> >();
		
		String line = "";
		while ((line = br.readLine()) != null) {
			String[] lbhb = line.split(",");
			lbhb[0] = lbhb[0].trim();
			lbhb[1] = lbhb[1].trim();
			
			HTMid lb, hb;
			if (lbhb[0].charAt(0) != 'N' && lbhb[0].charAt(0) != 'S') {
				lb = new HTMid(Long.parseLong(lbhb[0]));
			} else {
				lb = new HTMid(lbhb[0]);
			}
			if (lbhb[1].charAt(0) != 'N' && lbhb[1].charAt(0) != 'S') {
				hb = new HTMid(Long.parseLong(lbhb[1]));
			} else {
				hb = new HTMid(lbhb[1]);
			}
			pairList.add(new Pair<HTMid, HTMid>(lb, hb));
		}
		
		br.close();
		return new HTMRanges(pairList);
	}
	
	public List<Pair<HTMid, HTMid> > getPairList() {
		List<Pair<HTMid, HTMid> > duplicate = new ArrayList<Pair<HTMid, HTMid> >();
		duplicate.addAll(pairList);
		return duplicate;
	}
	
	/**
	 * Judge if Trixels represented by HTMRanges contain a certain Trixel  
	 * @param htmId HTMid of the Trixel to judge
	 * @return
	 */
	public boolean contains(HTMid htmId) {
		int thatLevel = htmId.getLevel();
		long hid = htmId.getId();
		if (level <= thatLevel) {
			for (Pair<HTMid, HTMid> pair : pairList) {
				long lb = pair.a.extend(thatLevel).a.getId();
				long hb = pair.b.extend(thatLevel).b.getId();
				if (lb <= hid && hid <= hb) {
					return true;
				}
			}
		} else if (level > thatLevel) {
			Pair<HTMid, HTMid> thatPair = htmId.extend(level);
			for (Pair<HTMid, HTMid> pair : pairList) {
				long lb = pair.a.getId();
				long hb = pair.b.getId();
				long thatLb = thatPair.a.getId();
				long thatHb = thatPair.b.getId();
				if (lb <= thatLb && thatHb <= hb) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean overlaps(HTMid htmId) {
        int thatLevel = htmId.getLevel();
        long hid = htmId.getId();
        if (level <= thatLevel) {
            for (Pair<HTMid, HTMid> pair : pairList) {
                long lb = pair.a.extend(thatLevel).a.getId();
                long hb = pair.b.extend(thatLevel).b.getId();
                if (lb <= hid && hid <= hb) {
                    return true;
                }
            }
        } else if (level > thatLevel){
            Pair<HTMid, HTMid> thatPair = htmId.extend(level);
            for (Pair<HTMid, HTMid> pair : pairList) {
                long lb = pair.a.getId();
                long hb = pair.b.getId();
                long thatLb = thatPair.a.getId();
                long thatHb = thatPair.b.getId();
                if (thatLb >= lb && thatLb <= hb ||
                        thatHb >= lb && thatHb <= hb ||
                        lb >= thatLb && lb <= thatHb ||
                        hb >= thatLb && hb <= thatHb) {
                    return true;
                }
            }
        }
        return false;
    }
	
	/**
	 * Sort the pairs under these rules:
	 * 1. Sort in ascending order of lower bounds
	 * 2. If having same lower bounds, sort in ascending order of upper bounds
	 */
	private void sort() {
		Collections.sort(pairList, new Comparator<Pair<HTMid, HTMid> >() {
			@Override
			public int compare(Pair<HTMid, HTMid> o1, Pair<HTMid, HTMid> o2) {
				int cmp = o1.a.compareTo(o2.a);
				if (cmp != 0) {
					return cmp;
				}
				return o1.b.compareTo(o2.b);
			}
		});
	}
	
	/**
	 * Rearrange the pair list
	 * Merge pairs who are overlapping or adjacent with each other
	 * Must be called after sort
	 */
	private void compact() {
		List<Pair<HTMid, HTMid> > newList = new ArrayList<Pair<HTMid, HTMid> >();
		Iterator<Pair<HTMid, HTMid> > iter = pairList.iterator();
		if (!iter.hasNext()) {
			return;
		}
		Pair<HTMid, HTMid> first = iter.next();
		long lowerBound = first.a.hid;
		long higherBound = first.b.hid;
		while (iter.hasNext()) {
			Pair<HTMid, HTMid> now = iter.next();
			if (higherBound >= now.a.hid - 1) {
				higherBound = now.b.hid;
			} else {
				Pair<HTMid, HTMid> newPair = new Pair<HTMid, HTMid>(
						new HTMid(lowerBound),
						new HTMid(higherBound));
				newList.add(newPair);
				lowerBound = now.a.hid;
				higherBound = now.b.hid;
			}
		}
		Pair<HTMid, HTMid> newPair = new Pair<HTMid, HTMid>(
				new HTMid(lowerBound),
				new HTMid(higherBound));
		newList.add(newPair);
		pairList = newList;
	}

}
