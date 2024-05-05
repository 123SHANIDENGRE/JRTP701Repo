package com.nokia.nsw.uiv.adapter.si.ssh;

import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.nokia.nsw.uiv.adapter.si.sftp.NFMPL2VPNSftpClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NFMPL2VPNSshClient {

	private final NFMPL2VPNSshSeedParam sshSeedParam;
	
	 private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNSshClient.class);

	public NFMPL2VPNSshClient(NFMPL2VPNSshSeedParam sshSeedParam) {
		this.sshSeedParam = sshSeedParam;
	}

	public void doExec(String command) throws Exception {

		Session session = null;
		ChannelExec channel = null;

		try {
			session = new JSch().getSession(this.sshSeedParam.getUserName(), this.sshSeedParam.getHostName(),
					this.sshSeedParam.getPort());
			session.setPassword(this.sshSeedParam.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand(command);
			ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
			channel.setOutputStream(responseStream);
			channel.connect();

			while (channel.isConnected()) {
				Thread.sleep(100);
			}

			String responseString = new String(responseStream.toByteArray());
			log.info(responseString);
		} finally {
			if (session != null) {
				session.disconnect();
			}
			if (channel != null) {
				channel.disconnect();
			}
		}
	}
}
