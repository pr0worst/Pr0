package com.pr0gramm.app.api.pr0gramm;

import com.pr0gramm.app.api.pr0gramm.response.Feed;
import com.pr0gramm.app.api.pr0gramm.response.Login;
import com.pr0gramm.app.api.pr0gramm.response.NewComment;
import com.pr0gramm.app.api.pr0gramm.response.NewTag;
import com.pr0gramm.app.api.pr0gramm.response.Post;
import com.pr0gramm.app.api.pr0gramm.response.Sync;
import com.pr0gramm.app.feed.Nothing;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;
import rx.Observable;

/**
 */
public interface Api {
    @GET("/api/items/get")
    Observable<Feed> itemsGet(
            @Query("promoted") int promoted,
            @Query("older") Long older,
            @Query("newer") Long newer,
            @Query("id") Long around,
            @Query("flags") int flags,
            @Query("tags") String tags,
            @Query("likes") String likes,
            @Query("self") String self,
            @Query("user") String user);

    @FormUrlEncoded
    @POST("/api/items/vote")
    Observable<Nothing> vote(@Field("_nonce") Nonce nonce,
                             @Field("id") long id, @Field("vote") int voteValue);

    @FormUrlEncoded
    @POST("/api/tags/vote")
    Observable<Nothing> voteTag(@Field("_nonce") Nonce nonce,
                                @Field("id") long id, @Field("vote") int voteValue);

    @FormUrlEncoded
    @POST("/api/comments/vote")
    Observable<Nothing> voteComment(@Field("_nonce") Nonce nonce,
                                    @Field("id") long id, @Field("vote") int voteValue);

    @FormUrlEncoded
    @POST("/api/user/login")
    Observable<Login> login(
            @Field("name") String username,
            @Field("password") String password);

    @FormUrlEncoded
    @POST("/api/tags/add")
    Observable<NewTag> addTags(
            @Field("_nonce") Nonce nonce,
            @Field("itemId") long lastId,
            @Field("tags") String tags);

    @FormUrlEncoded
    @POST("/api/comments/post")
    Observable<NewComment> postComment(
            @Field("_nonce") Nonce nonce,
            @Field("itemId") long itemId,
            @Field("parentId") long parentId,
            @Field("comment") String comment);

    @GET("/api/items/info")
    Observable<Post> info(@Query("itemId") long itemId);


    @GET("/api/user/sync")
    Sync sync(@Query("lastId") long lastId);

    @GET("/api/profile/info")
    Info info(@Query("name") String name);

    public static class Nonce {
        public final String value;

        public Nonce(String userId) {
            this.value = userId.substring(0, 16);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
