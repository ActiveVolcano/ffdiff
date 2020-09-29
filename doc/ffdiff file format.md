.ffdiff File Format
=======

> ffdiff stands for Fast File DIFFerence

Constraints
===========

* Network byte order (big endian).
* UTF-8 encoding.

Sections
========

Header section
------

| Field Name                     | Data Type | Bytes | Description                              |
| ------------------------------ | --------- | ----- | ---------------------------------------- |
| Magic Number                   | bytea(3)  | 3     | \xffd1ff                                 |
| File format version            | bytea(1)  | 1     | \x00                                     |
| Section Content Size           | int1      | 1     | 27 (\x1b) or 59 (\x3b)                   |
| Base File Size                 | int8      | 8     |                                          |
| Target File Size               | int8      | 8     |                                          |
| Target File Timestamp          | timestamp | 8     | Microsecond timestamp.                   |
| Target File POSIX Permissions   | bytea(2)  | 2     | Every 4 bits: -ugo                       |
| Target File Windows Attributes | byte      | 1     | Every bit: RASH----                    |
| Password Hash (Optional)       | bytea(32) | 32    | SHA256 ({Password} + {Target File Size}) |

Target File POSIX Permissions (LSB):

* reserved
* u - user (-rwx)
* g - group (-rwx)
* o - others (-rwx)

Every bit in permissions (MSB):

* reserved
* r - read
* w - write
* x - execute

Target File Windows Attributes (LSB):

* R - Read-only
* A - Archive
* S - System
* H - Hidden

Password Hash:

* e.g. Password = 'OK', Target File Size = 1234 Bytes, then Password Hash = SHA256 ('OK1234').
* Unencrypted file has no Password Hash field, so Section Content Size = 24; otherwise 56.

CP24 Section
----

Copy part of base file to target file.

| Field Name           | Data Type | Bytes | Description                                       |
| -------------------- | --------- | ----- | ------------------------------------------------- |
| Section Name         | char(4)   | 4     | 'CP24'                                            |
| Section Content Size | int1      | 1     | 11                                                |
| Copy Offset          | int4      | 4     | Copy offset of base file.                         |
| Copy Length          | int3      | 3     | Copy length of base file.                         |
| Checksum             | bytea(4)  | 4     | First 4 bytes of MD5 checksum of the copied part. |

CP32 Section
----

Copy part of base file to target file.

| Field Name           | Data Type | Bytes | Description                      |
| -------------------- | --------- | ----- | -------------------------------- |
| Section Name         | char(4)   | 4     | 'CP32'                           |
| Section Content Size | int1      | 1     | 27                               |
| Copy Offset          | int7      | 7     | Copy offset of base file.        |
| Copy Length          | int4      | 4     | Copy length of base file.        |
| Checksum             | bytea(16) | 16    | MD5 checksum of the copied part. |

DIFF Section
----

Difference data.

| Field Name            | Data Type | Bytes | Description                                    |
| --------------------- | --------- | ----- | ---------------------------------------------- |
| Section Name          | char(4)   | 4     | 'DIFF'                                         |
| Section Content Size  | int4      | 4     | Total size of all following fields.            |
| Compression Algorithm | char(1)   | 1     | See below.                                     |
| Encryption Algorithm  | char(1)   | 1     | See below.                                     |
| Original Data Size    | int4      | 4     | Size of the original difference data.          |
| Checksum              | bytea(16) | 16    | MD5 checksum of the original difference data.  |
| Cooked Data           | bytea     | ?     | Compressed and then encrypted difference data. |

Compression algorithm:

* 'N' - No compression
* 'D' - DEFLATE
* '7' - LZMA

Encryption algorithm:

* 'N' - No encryption
* 'A' - USA standard AES
* 'S' - China standard SM4

Key = MD5 (Password), Initial Vector = MD5 (Key).