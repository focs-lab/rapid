package event;

public enum EventType {
	ACQUIRE, RELEASE, READ, WRITE, FORK, JOIN, BEGIN, END, DUMMY;

	public boolean isAcquire() {
		return this.ordinal() == ACQUIRE.ordinal();
	}

	public boolean isRelease() {
		return this.ordinal() == RELEASE.ordinal();
	}

	public boolean isRead() {
		return this.ordinal() == READ.ordinal();
	}

	public boolean isWrite() {
		return this.ordinal() == WRITE.ordinal();
	}

	public boolean isFork() {
		return this.ordinal() == FORK.ordinal();
	}

	public boolean isJoin() {
		return this.ordinal() == JOIN.ordinal();
	}

	public boolean isBegin() {
		return this.ordinal() == BEGIN.ordinal();
	}

	public boolean isEnd() {
		return this.ordinal() == END.ordinal();
	}

	public boolean isLockType() {
		return this.isAcquire() || this.isRelease();
	}

	public boolean isAccessType() {
		return this.isRead() || this.isWrite();
	}

	public boolean isExtremeType() {
		return this.isFork() || this.isJoin();
	}

	/*
	 * public boolean isSyncType() { return isLockType() || isExtremeType(); }
	 */

	public boolean isTransactionType() {
		return isBegin() || isEnd();
	}

	public boolean isDummyType() {
		return this.ordinal() == DUMMY.ordinal();
	}

	public static boolean conflicting(EventType et1, EventType et2) {
		return et1.isAccessType() && et2.isAccessType()
				&& (et1.isWrite() || et2.isWrite());
	}

	public String toString() {
		String str = "";
		if (this.isAcquire())
			str = "ACQUIRE";
		if (this.isRelease())
			str = "RELEASE";
		if (this.isRead())
			str = "READ";
		if (this.isWrite())
			str = "WRITE";
		if (this.isFork())
			str = "FORK";
		if (this.isJoin())
			str = "JOIN";
		if (this.isBegin())
			str = "BEGIN";
		if (this.isEnd())
			str = "END";
		return str;
	}

	public String toStandardFormat() {
		String str = "";
		if (this.isAcquire())
			str = "acq";
		if (this.isRelease())
			str = "rel";
		if (this.isRead())
			str = "r";
		if (this.isWrite())
			str = "w";
		if (this.isFork())
			str = "fork";
		if (this.isJoin())
			str = "join";
		if (this.isBegin())
			str = "begin";
		if (this.isEnd())
			str = "end";
		return str;
	}
}
