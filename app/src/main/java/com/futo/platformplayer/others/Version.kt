package com.futo.platformplayer.others

data class Version(val upstreamMajor: Int, val forkMinor: Int) {
    override fun toString(): String {
        if (forkMinor == 0) {
            return upstreamMajor.toString();
        }
        return "${upstreamMajor}.${forkMinor}";
    }

    override fun equals(other: Any?): Boolean {
        return (
            other is Version &&
            this.upstreamMajor == other.upstreamMajor &&
            this.forkMinor == other.forkMinor
        );
    }

    operator fun compareTo(other: Version): Int {
        val major = this.upstreamMajor - other.upstreamMajor;
        val patch = this.forkMinor - other.forkMinor;
        return if (major != 0) major else patch;
    }

    override fun hashCode(): Int {
        return upstreamMajor * 10_000 + forkMinor;
    }

    companion object {
        fun fromString(input: String): Version {
            val parts = input.split(".", limit = 2);
            return Version(
                parts[0].toInt(),
                if (parts.size > 1) parts[1].toInt() else 0
            );
        }
    }
}
