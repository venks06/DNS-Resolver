package com.fcn.dns;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

public class mydnsresolver {

	static class Root {
		String rname;
		String address;
		String mac;
		String name;

		public Root(String rname, String address, String mac, String name) {
			this.rname = rname;
			this.address = address;
			this.mac = mac;
			this.name = name;
		}
	}
	
	static int index = 0;
	
	static String[] topSites = new String[] {
		"www.google.com", "www.facebook.com", "www.youtube.com", "www.stackoverflow.com", "www.yahoo.com", "www.amazon.com",
		"www.wikipedia.org", "www.quora.com", "www.google.co.in", "www.twitter.com", "www.live.com", "www.world.taobao.com",
		"www.netflix.com", "www.msn.com", "www.yahoo.co.jp", "www.linkedin.com", "www.google.co.jp", "www.dailymotion.com",
		"www.bing.com", "www.vk.com", "www.yandex.ru", "www.hbo.com", "www.ebay.com", "www.instagram.com", "www.google.de"
	};

	public static ArrayList<Root> readRootsFile() {
		ArrayList<Root> roots = new ArrayList<Root>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("roots"));
			String line = null, rootArray[];
			try {
				while (null != (line = br.readLine())) {
					rootArray = line.split(";");
					roots.add(new Root(rootArray[0], rootArray[1],
							rootArray[2], rootArray[3]));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return roots;
	}
	
	public static void writeToFile(String msg) {
		BufferedWriter output = null;
		File file = null;
		try {
			file = new File("mydig_output.txt");
			msg += "\n";
			output = new BufferedWriter(new FileWriter(file, true));
			output.write(msg);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
					output = null;
					file = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static boolean resolve(String rootServer, String hostName) {
		try {
			SimpleResolver sr = new SimpleResolver(rootServer);
			Name name = Name.fromString(hostName, Name.root);
			Record record = Record.newRecord(name, Type.A, DClass.IN, Section.AUTHORITY);
			Message m = Message.newQuery(record);
			Message response = sr.send(m);
			Record[] answerSection = response.getSectionArray(Section.ANSWER);
			Record[] authoritySection;
			while (answerSection.length == 0) {
				authoritySection = response.getSectionArray(Section.AUTHORITY);
				if (authoritySection.length == 0)
					return false;
				boolean isTimeout = false;
				for (int i = 0; i < authoritySection.length; i++) {
					try {
						sr = new SimpleResolver(authoritySection[i].rdataToString());
						response = sr.send(m);
						isTimeout = false;
						break;
					} catch (SocketTimeoutException ex) {
						isTimeout = true;
					}
				}
				if(isTimeout)
					break;
				answerSection = response.getSectionArray(Section.ANSWER);
			}
			
			for (Record r : answerSection) {
				if (r.getType() == Type.A) {
					System.out.println(r.rdataToString());
					return true;
				}
			}

			for (Record r : answerSection) {
				if (r.getType() == Type.CNAME)
					return resolve(rootServer, r.rdataToString());
			}
			System.out.println();
		} catch (SocketTimeoutException e) {
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	static HashSet<String> servers = new HashSet<String>();
	public static boolean buildDigTool(String localDNSServer, String hostName, int type) {
		try {
			SimpleResolver sr = new SimpleResolver(localDNSServer);
			Name name = Name.fromString(hostName, Name.root);
			Record record = Record.newRecord(name, type, DClass.IN, Section.AUTHORITY);
			Message m = Message.newQuery(record);
			Message response = sr.send(m);
			Record[] answerSection = response.getSectionArray(Section.ANSWER);
			Record[] authoritySection;
			while (answerSection.length == 0) {
				authoritySection = response.getSectionArray(Section.AUTHORITY);
				if (authoritySection.length == 0)
					return false;
				String rString = authoritySection[0].rdataToString();
				
				if(rString.contains(" ")) {
					rString = rString.split(" ")[0];
				}
				if(servers.contains(rString))
					break;
				else
					servers.add(rString);
				sr = new SimpleResolver(rString);
				response = sr.send(m);
				answerSection = response.getSectionArray(Section.ANSWER);
			}
			
			if(answerSection.length == 1 && answerSection[0].getType() == Type.CNAME) {
				buildDigTool(localDNSServer, answerSection[0].rdataToString(), type);
			} else {
				String data = "", typeName = "";
				for(int i = 0; i < answerSection.length; i++) {
					if(answerSection[i].getType() == Type.CNAME)
						typeName = "CNAME\t";
					else if(answerSection[i].getType() == Type.A)
						typeName = "A\t";
					else if(answerSection[i].getType() == Type.NS)
						typeName = "NS\t";
					else if(answerSection[i].getType() == Type.MX)
						typeName = "MX\t";
					data += typeName + answerSection[i].rdataToString() + "\n";
				}
				if(answerSection.length == 0) {
					Iterator<String> it = servers.iterator();
					String reqType = "";
					if(type == Type.NS)
						reqType = "NS\t";
					else if(type == Type.MX)
						reqType = "MX\t";
					while(it.hasNext()) {
						data += reqType + it.next() + "\n";
					}
				}
				writeToFile(data);
				System.out.println(data);
			}
		} catch (SocketTimeoutException e) {
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;		
	}
	
	public static void calculateResponseTimes(ArrayList<Root> roots, boolean isLocalDNSServer, boolean isPublicDNSServer) {
		ArrayList<ArrayList<Double>> responseTimes = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> unitTime = new ArrayList<Double>();
		long start, duration;
		for (int i = 0; i < topSites.length; i++) {
			System.out.println(topSites[i]);
			for (int j = 0; j < 10; j++) {
				start = System.currentTimeMillis();
				if(isLocalDNSServer) {
					resolve("130.245.255.4", topSites[i]);
				} else if(isPublicDNSServer) {
					resolve("8.8.8.8", topSites[i]);
				} else {
					for (Root root : roots) {
						if (resolve(root.address, topSites[i]))
							break;
					}
				}
				duration = System.currentTimeMillis() - start;
				System.out.println(Double.valueOf(duration));
				unitTime.add(Double.valueOf(duration));
			}
			responseTimes.add(unitTime);
			unitTime = new ArrayList<Double>();
		}
		System.out.println("Calculating average");
		for (int i = 0; i < topSites.length; i++) {
			//System.out.println(topSites[i]);
			double avg = 0, sum = 0;
			ArrayList<Double> a = responseTimes.get(i);
			for (int j = 0; j < 10; j++) {
				// System.out.println(a.get(j));
				sum += a.get(j);
			}
			avg = sum / 10;
			System.out.println(avg);
		}
	}

	public static void main(String[] args) {
		if(null == args || args.length != 1) {
			System.out.println("Invalid arguments");
			return;
		}
		ArrayList<Root> roots = readRootsFile();
		// Part A
		for(int i = 0; i < roots.size(); i++) {
			if(resolve(roots.get(i).address, "www.cs.stonybrook.edu"))
				break;
		}
		// Part B
		// buildDigTool("8.8.8.8", "www.stonybrook.edu", Type.MX);
		// calculateResponseTimes(roots, true, false);
	}
}
