package de.uniko.sebschlicht.graphity.benchmark.api;

public class RequestComposition {

    private float feed;

    private float follow;

    private float unfollow;

    private float post;

    /**
     * Creates a transferable request composition object.
     * Parameters expect the percentage of a certain request type during the
     * benchmark.
     * 
     * @example new RequestComposition(0, 30.0, 20.0, 50.0);
     * 
     * @param feed
     *            percentage of news feed retrieval requests
     * @param follow
     *            percentage of follow requests
     * @param unfollow
     *            percentage of unfollow requests
     * @param post
     *            percentage of status update post requests
     */
    public RequestComposition(
            float feed,
            float follow,
            float unfollow,
            float post) {
        this.feed = feed;
        this.follow = follow;
        this.unfollow = unfollow;
        this.post = post;
    }

    /**
     * empty constructor for deserialization
     */
    public RequestComposition() {
    }

    /**
     * @return percentage of news feed retrieval requests
     */
    public float getFeed() {
        return feed;
    }

    /**
     * @return percentage of follow requests
     */
    public float getFollow() {
        return follow;
    }

    /**
     * @return percentage of unfollow requests
     */
    public float getUnfollow() {
        return unfollow;
    }

    /**
     * @return percentage of status update post requests
     */
    public float getPost() {
        return post;
    }
}