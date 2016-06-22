package com.fcn.dns;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Iterator;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

public class mydig {

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

	public static void main(String[] args) {
		if(null == args || args.length != 2) {
			System.out.println("Invalid arguments");
			return;
		}
		// Part B
		int rType = Type.A;
		if(args[1] == "MX")
			rType = Type.MX;
		else if(args[1] == "NS")
			rType = Type.NS;
		 buildDigTool("8.8.8.8", args[0], rType);
	}
}
