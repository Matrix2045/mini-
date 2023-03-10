package org.minima.system.commands.base;

import java.util.ArrayList;
import java.util.Arrays;

import org.minima.database.mmr.MMR;
import org.minima.database.mmr.MMRData;
import org.minima.database.mmr.MMREntryNumber;
import org.minima.database.mmr.MMRProof;
import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniString;
import org.minima.system.commands.Command;
import org.minima.utils.Crypto;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;

public class mmrcreate extends Command {

	public class mmrleafnode{
		
		public int mEntry;
		public String mInput;
		public String mData;
		public MiniData mHash;
		
		public mmrleafnode() {}
	}
	
	public mmrcreate() {
		super("mmrcreate","[nodes:[]] - Create an MMR Tree of data. Nodes can be STRING / HEX");
	}
	
	@Override
	public String getFullHelp() {
		return "\nmmrcreate\n"
				+ "\n"
				+ "Create an MMR Tree of data. Can be used in MAST contracts.\n"
				+ "\n"
				+ "Must specify a JSON array of string/HEX data for the leaf nodes.\n"
				+ "\n"
				+ "They could be a list of public keys.. and then in a script you can check if the given key is one in the set.\n"
				+ "\n"
				+ "OR you can have different scripts.. and then you can execute any number of scripts from the same UTXO.\n"
				+ "\n"
				+ "Returns the MMR data and proof for each leaf node and the MMR root hash.\n"
				+ "\n"
				+ "nodes:\n"
				+ "    JSON array of string/HEX data for the leaf nodes.\n"
				+ "\n"
				+ "Examples:\n"
				+ "\n"
				+ "mmrcreate nodes:[\"RETURN TRUE\",\"RETURN FALSE\"]\n"
				+ "\n"
				+ "mmrcreate nodes:[\"0xFF..\",\"0xEE..\"]\n";
	}
	
	@Override
	public ArrayList<String> getValidParams(){
		return new ArrayList<>(Arrays.asList(new String[]{"nodes"}));
	}
	
	@Override
	public JSONObject runCommand() throws Exception{
		JSONObject ret = getJSONReply();
		
		if(!existsParam("nodes")) {
			throw new Exception("MUST Specify a nodes JSONArray");
		}
		
		JSONArray mmrdata = getJSONArrayParam("nodes");
		
		//Create an MMR
		MMR mmrtree = new MMR();
		
		//Add the Signatures
		int counter=0;
		ArrayList<mmrleafnode> leafnodes = new ArrayList<>();
		for(Object data : mmrdata) {
			
			//The String sig
			String strdata = (String)data;
			
			//Is it HEX
			MiniData mdata = null;
			if(strdata.startsWith("0x")) {
				mdata = new MiniData(strdata);
			}else {
				mdata = new MiniData( new MiniString(strdata).getData() );
			}
			
			//Hash it..
			MiniData hash = Crypto.getInstance().hashObject(mdata);
			
			//Create leafnode
			mmrleafnode leafnode = new mmrleafnode();
			leafnode.mEntry = counter;
			leafnode.mInput = strdata;
			leafnode.mData  = mdata.to0xString();
			leafnode.mHash 	= hash;
			
			//Add them to our list
			leafnodes.add(leafnode);
			
			//Increment
			counter++;
			
			//Add to the MMR
			mmrtree.addEntry(new MMRData(hash));
		}
		
		//Get the root..
		MiniData root = mmrtree.getRoot().getData();
		
		//Now create the output
		JSONArray leafdata = new JSONArray();
		for(mmrleafnode leaf : leafnodes) {
			
			JSONObject jobj = new JSONObject();
//			jobj.put("entry", leaf.mEntry);
//			jobj.put("input", leaf.mInput);
			jobj.put("data", leaf.mData);	
//			jobj.put("hash", leaf.mHash);
			
			//Get the proof..
			MMRProof proof = mmrtree.getProof(new MMREntryNumber(leaf.mEntry));
			
			//Get as data string
			MiniData dataproof = MiniData.getMiniDataVersion(proof); 
			
			//Add this proof
			jobj.put("proof", dataproof.to0xString());
			
			//Add to the total
			leafdata.add(jobj);
		}
		
		JSONObject jsonmmr = new JSONObject();
		jsonmmr.put("nodes", leafdata);
		jsonmmr.put("total", leafnodes.size());
		jsonmmr.put("root", root.to0xString());
		
		//Add balance..
		ret.put("response", jsonmmr);
		
		return ret;
	}

	@Override
	public Command getFunction() {
		return new mmrcreate();
	}

}
