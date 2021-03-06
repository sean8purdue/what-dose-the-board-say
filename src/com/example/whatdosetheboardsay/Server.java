package com.example.whatdosetheboardsay;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import android.annotation.SuppressLint;
import android.util.Log;


public class Server implements Runnable {

	public static String ServerIP = "127.0.0.1";
	public static ArrayList<String> addList = new ArrayList<String>();
	public static ArrayList<Integer> badID = new ArrayList<Integer>();
	public static int clientCount = 0;
	public static String password = null;
	public static DatagramSocket socket;
	public static ServerSendMsg ssm;
	public static boolean exit = false;
	public void SetPremit(int clientID, int YesNo){
		if(YesNo==1){
			badID.add(clientID);
		}else{
			try{
				badID.remove(clientID);
			}catch(Exception e){
				return;
			}
		}
	}
	public int GetClientCount(){
		return clientCount;
	}
	
	public int[] GetClientPremit(){
		if(clientCount==0)
			return null;
		int[] rev = new int[clientCount];
		for(int i=0; i<clientCount; i++)
			if(badID.contains(i))
				rev[i] = 0;
			else
				rev[i] = 1;
		return rev;
	}
	
	@SuppressLint("NewApi")
	@Override
	public void run() {
		ServerIP = GDB_sc.GetLocalIpAddress();
		byte[] bufsize = new byte[4];
		try {
			GDB_sc.setServer();
			InetAddress serverAddr = InetAddress.getByName(ServerIP);
			Log.d("UDP", "S: Connecting...");
			socket = new DatagramSocket(2333, serverAddr);
			InetAddress returnAddr;
			DatagramPacket returnPacket;
			ssm = new ServerSendMsg() ;
			new Thread(ssm).start();
			while (true) {
				if (exit) {
					socket.close();
					ssm.exit = true;
					//ssm.sendMessage(null);
					break;
				}
				DatagramPacket packet = new DatagramPacket(bufsize, 4);
				Log.d("UDP", "S: Receiving datasize...");
				socket.receive(packet);
				Log.d("UDP", "S: Received 1 :" + bufsize[0]);
				Log.d("UDP", "S: Received 2 :" + bufsize[1]);
				Log.d("UDP", "S: Received 3 :" + bufsize[2]);
				Log.d("UDP", "S: Received 4 :" + bufsize[3]);
				Log.d("UDP", "S: Received :"
						+ ByteBuffer.wrap(bufsize).getInt());
				if (ByteBuffer.wrap(bufsize).getInt() > 0x1000000)
					continue;
				byte[] buf = new byte[ByteBuffer.wrap(bufsize).getInt()];
				// delete packet;
				packet = new DatagramPacket(buf, buf.length);
				Log.d("UDP", "S: Receiving data...");
				socket.receive(packet);
				Log.d("UDP", "S: Received: '" + new String(packet.getData())
						+ "'");
				String attempt = new String(packet.getData());
				Log.d("UDP", "S: Done. Testing the Attempt");
				/* Test 1, No pass */
				if (attempt.charAt(0) == '|') {
					attempt = attempt.substring(1);
					if (password == null || password.length() == 0) {
						if (attempt.indexOf('|') == -1) {
							addList.add(attempt);
							returnAddr = InetAddress.getByName(attempt);
							Log.d("UDP", "S: No Pass - No Pass, add");
						} else {
							addList.add(attempt.split("\\|")[0]);
							returnAddr = InetAddress.getByName(attempt
									.split("\\|")[0]);
							Log.d("UDP", "S: No Pass - Unused Pass, add");
						}
						returnPacket = new DatagramPacket(ByteBuffer
								.allocate(4).putInt(clientCount).array(), 4,
								returnAddr, 2333);
						clientCount++;
						Log.d("UDP", "S: YYYYYYYYYYES");
					}
					/* Test 2, Has pass */
					else if (attempt.indexOf('|') == -1) {/* Pass miss */
						returnAddr = InetAddress.getByName(attempt);
						Log.d("UDP", "S: Has Pass - No Pass, reject");
						returnPacket = new DatagramPacket(ByteBuffer
								.allocate(4).putInt(-1).array(), 4, returnAddr,
								2333);
						socket.send(returnPacket);
					} else {
						returnAddr = InetAddress
								.getByName(attempt.split("\\|")[0]);
						Log.d("PASS",
								"split: "
										+ Arrays.toString(attempt.split("\\|")));
						Log.d("PASS", "retIPadd " + returnAddr);
						Log.d("PASS", "attempt: " + attempt);
						Log.d("PASS", "attpass: " + attempt.split("\\|")[1]);
						Log.d("PASS",
								"servers: "
										+ EncryptUtil.encrypt(
												attempt.split("\\|")[0]
														+ password,
												EncryptUtil.SHA1));
						if (attempt.split("\\|")[1].equals(EncryptUtil.encrypt(
								attempt.split("\\|")[0] + password,
								EncryptUtil.SHA1))) {
							Log.d("UDP", "S: Has Pass - Valid Pass, add");
							addList.add(attempt.split("\\|")[0]);
							returnPacket = new DatagramPacket(ByteBuffer
									.allocate(4).putInt(clientCount).array(),
									4, returnAddr, 2333);
							clientCount++;
						} else {
							Log.d("UDP", "S: Has Pass - Wrong Pass, reject");
							returnPacket = new DatagramPacket(ByteBuffer
									.allocate(4).putInt(-1).array(), 4,
									returnAddr, 2333);
						}
					}
					socket.send(returnPacket);
				} else {
					byte rvdata[] = packet.getData();
					int recvIDint = rvdata[0] - '0';
					byte[] toall = Arrays.copyOfRange(rvdata, 2, rvdata.length);
					if(badID.contains(recvIDint))
						continue;
					for (int i = 0; i < clientCount; i++) {
						if (recvIDint == i)
							continue;
						InetAddress clientAddr = InetAddress.getByName(addList
								.get(i));
						DatagramPacket packetsize = new DatagramPacket(
								ByteBuffer.allocate(4).putInt(toall.length)
										.array(), 4, clientAddr, 2333);
						socket.send(packetsize);
						packetsize = new DatagramPacket(toall, toall.length,
								clientAddr, 2333);
						socket.send(packetsize);
					}
					GDB_sc.reciveByteMessage(toall); // need optimize
				}
			}
		} catch (Exception e) {
			Log.e("UDP", "S: Error", e);
		}

	}
}

class ServerSendMsg implements Runnable {
	public byte[] toall;
	public static boolean exit = false;

	public void sendMessage(byte[] toalll) {
		toall = toalll;
		Log.d("S: send", "init");
		synchronized (this) {
			this.notify();
		}
		
	}
	
	public void run() {
		try {
			while (true) {
				Log.d("S: send", "init start");
				synchronized (this) {
					this.wait();
				}
				if (exit) {
					break;
				}
				Log.d("S: send", "start");
				for (int i = 0; i < Server.clientCount; i++) {
					Log.d("S: send", "current "+i);
					InetAddress clientAddr = InetAddress.getByName(Server.addList
							.get(i));
					DatagramPacket packetsize = new DatagramPacket(
							ByteBuffer.allocate(4).putInt(toall.length)
									.array(), 4, clientAddr, 2333);
					Server.socket.send(packetsize);
					packetsize = new DatagramPacket(toall, toall.length,
							clientAddr, 2333);
					Server.socket.send(packetsize);
				}
			}
		} catch (Exception e) {
			Log.d("S: ", "ERROR");
		}
	}
}
