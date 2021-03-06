package com.pr0gramm.app.ui;

import android.net.Uri;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import com.pr0gramm.app.feed.FeedFilter;
import com.pr0gramm.app.feed.FeedType;

import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
*/
public class FeedFilterWithStart {
    private final FeedFilter filter;
    private final Optional<Long> start;

    FeedFilterWithStart(FeedFilter filter, Long start) {
        this.filter = filter;
        this.start = Optional.fromNullable(start != null && start > 0 ? start : null);
    }

    public FeedFilter getFilter() {
        return filter;
    }

    public Optional<Long> getStart() {
        return start;
    }


    public static Optional<FeedFilterWithStart> fromUri(Uri uri) {
        List<Pattern> patterns = ImmutableList.of(pFeed, pFeedId, pUserUploads, pUserUploadsId, pTag, pTagId);

        // get the path without optional comment link
        String path = uri.getPath().replaceFirst(":.*$", "");
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(path);
            if (!matcher.matches())
                continue;

            Map<String, String> groups = matcher.namedGroups();

            FeedFilter filter = new FeedFilter().withFeedType(FeedType.NEW);

            if ("top".equals(groups.get("type")))
                filter = filter.withFeedType(FeedType.PROMOTED);

            // filter by user
            String user = groups.get("user");
            if (!Strings.isNullOrEmpty(user))
                filter = filter.withUser(user);

            // filter by tag
            String tag = groups.get("tag");
            if (!Strings.isNullOrEmpty(tag))
                filter = filter.withTags(tag);

            Long start = Longs.tryParse(firstNonNull(groups.get("id"), "-1"));
            return Optional.of(new FeedFilterWithStart(filter, start));
        }

        return Optional.absent();
    }

    private static final Pattern pFeed = Pattern.compile("^/(?<type>new|top)$");
    private static final Pattern pFeedId = Pattern.compile("^/(?<type>new|top)/(?<id>[0-9]+)$");
    private static final Pattern pUserUploads = Pattern.compile("^/user/(?<user>[^/]+)/uploads$");
    private static final Pattern pUserUploadsId = Pattern.compile("^/user/(?<user>[^/]+)/uploads/(?<id>[0-9]+)$");
    private static final Pattern pTag = Pattern.compile("^/(?<type>new|top)/(?<tag>[^/]+)$");
    private static final Pattern pTagId = Pattern.compile("^/(?<type>new|top)/(?<tag>[^/]+)/(?<id>[0-9]+)$");
}
