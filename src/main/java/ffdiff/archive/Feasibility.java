package ffdiff.archive;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 可行性研究
 * 
 * <pre>
create table temp_digest (off int8, len int4, md5 char(32), sha1 char(40));
truncate table temp_digest;
select count(*) from temp_digest_9 where sha1 in (select sha1 from temp_digest_7);
对比 rfmaze v2.12 v2.4
docker 镜像导出 tar 文件，每块 1 kB，v2.12 总共 584,745 块，其中 518,535 块与 v2.4 重复，重复率 89%
gzip 压缩后，仅有 16 块重复。
对比灾备平台 v0731 v0908
docker 镜像导出 tar 文件，每块 1 kB，v0908 总共 716,940 块，其中 580,218 块与 v0731 重复，重复率 81%
gzip 压缩后，无一块重复。
 * </pre>
 */
public class Feasibility {
	
	//------------------------------------------------------------------------
	private static final int BLOCK_SIZE = 1024;
	private static final String TSV_TEMPLATE = "%d\t%d\t\"%s\"\t\"%s\"\r\n";
	
	//------------------------------------------------------------------------
	private static Path pathIn, pathOut;

	//------------------------------------------------------------------------
	public static void main (String[] args) {
		if (! readCmdArgs (args)) {
			return;
		}
		
		byte[] buffer = new byte[BLOCK_SIZE];
		long offset = 0;
		try (InputStream  in  = Files.newInputStream    (pathIn, StandardOpenOption.READ);
		     Writer       out = Files.newBufferedWriter (pathOut,
		     StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (int read = in.read (buffer) ; read > 0 ; read = in.read (buffer)) {
				byte[] digest = read == buffer.length ? buffer : Arrays.copyOf (buffer, read);
				byte[] md5b = DigestUtils.md5 (digest), sha1b = DigestUtils.sha1 (digest);
				String sql = String.format
					(TSV_TEMPLATE, offset, read, Hex.encodeHexString (md5b), Hex.encodeHexString (sha1b));
				out.append (sql);
				offset += read;
			}
			
		} catch (IOException e) {
			System.err.println (ExceptionUtils.getMessage (e));
		}
	}
	
	//------------------------------------------------------------------------
	private static boolean readCmdArgs (String[] args) {
		if (args.length != 2 || StringUtils.isAllBlank(args)) {
			System.err.println ("Usage: java -jar ffdiff.jar {input file path} {output file path}");
			return false;
		}
		pathIn  = Paths.get (args[0]);
		pathOut = Paths.get (args[1]);
		return true;
	}

}
