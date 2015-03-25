package de.uniko.sebschlicht.graphity.benchmark.client.config;

/**
 * Request composition of the benchmark.
 * Specifies the probability of each request type.
 * 
 * @author sebschlicht
 * 
 */
public class RequestComposition {

    private float feed;

    private float follow;

    private float unfollow;

    private float post;

    private float user;

    /**
     * Creates a request composition object holding the percentage of the
     * probability of each request type during the benchmark.
     * 
     * @example new RequestComposition(0, 30.0, 10.0, 50.0, 10.0);
     * 
     * @param feed
     *            percentage of news feed retrieval requests
     * @param follow
     *            percentage of follow requests
     * @param unfollow
     *            percentage of unfollow requests
     * @param post
     *            percentage of status update post requests
     * @param user
     *            percentage of add user requests
     */
    public RequestComposition(
            float feed,
            float follow,
            float unfollow,
            float post,
            float user) {
        this.feed = feed;
        this.follow = follow;
        this.unfollow = unfollow;
        this.post = post;
        this.user = user;
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

    /**
     * @return percentage of add user requests
     */
    public float getUser() {
        return user;
    }
}
