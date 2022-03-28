package com.capitalone.dashboard.webhook.github;

public class GraphQLQuery {
    public static final String COMMITS_GRAPHQL =
            "query ($owner: String!, $name: String!, $oid: GitObjectID!) {" +
            "    repository(owner: $owner, name: $name) {" +
            "        object(oid: $oid) {" +
            "            ... on Commit {" +
            "                oid" +
            "                author {" +
            "                    name" +
            "                    date" +
            "                    user {\n" +
            "                        login\n" +
            "                        name\n" +
            "                    }\n" +
            "                }" +
            "                committer {" +
            "                    name" +
            "                    date" +
            "                    user {" +
            "                        login" +
            "                        name" +
            "                    }" +
            "                }" +
            "                parents(first: 10) {" +
            "                    nodes {" +
            "                        oid" +
            "                    }" +
            "                }" +
            "            }" +
            "        }" +
            "    }" +
            "}";

    public static final String COMMITS_LIST_GRAPHQL =
            "query ($owner: String!, $name: String!, $branch: String!, $since: GitTimestamp!, $fetchCount: Int!) {" +
            "   repository(owner: $owner, name: $name) {" +
            "       ref(qualifiedName: $branch) {" +
            "           target {" +
            "               ... on Commit {" +
            "                   history(since: $since, first: $fetchCount) { " +
            "                       pageInfo { " +
            "                           endCursor" +
            "                           hasNextPage" +
            "                       }" +
            "                       edges {" +
            "                           cursor" +
            "                           node {" +
            "                               oid" +
            "                               changedFiles" +
            "                               deletions" +
            "                               additions" +
            "                               parents(first:10) {" +
            "                                   nodes {" +
            "                                       oid" +
            "                                   }" +
            "                               }" +
            "                               message" +
            "                               committer {" +
            "                                   user {" +
            "                                       login" +
            "                                   }" +
            "                                   name" +
            "                                   date" +
            "                               }" +
            "                               author {" +
            "                                   name" +
            "                                   user {" +
            "                                       login" +
            "                                       name" +
            "                                   }" +
            "                                   email" +
            "                                   date" +
            "                               }" +
            "                               status {" +
            "                                   state" +
            "                                   contexts {" +
            "                                       id" +
            "                                       description" +
            "                                   }" +
            "                               }" +
            "                           }" +
            "                       }" +
            "                   }" +
            "               }" +
            "           }" +
            "       }" +
            "   }" +
            "}";


    public static final String PR_GRAPHQL_BEGIN_PRE =
            "query ($owner: String!, $name: String!, $number: Int!";

    public static final String PR_GRAPHQL_COMMITS_BEGIN = ", $commits: Int!";

    public static final String PR_GRAPHQL_COMMENTS_BEGIN = ", $comments: Int!";

    public static final String PR_GRAPHQL_BEGIN_POST =
            ") {" +
            "  repository(owner: $owner, name: $name) {" +
            "    pullRequest(number: $number) {";

    public static final String PR_GRAPHQL_COMMITS =
            "      commits(first: $commits) {" +
                    "        totalCount" +
                    "        nodes {" +
                    "          commit {" +
                    "            oid" +
                    "            committedDate" +
                    "            additions" +
                    "            deletions" +
                    "            changedFiles" +
                    "            message" +
                    "            status {" +
                    "              context(name: \"approvals/lgtmeow\") {" +
                    "                state" +
                    "                targetUrl" +
                    "                description" +
                    "                context" +
                    "              }" +
                    "            }" +
                    "            author {" +
                    "              name" +
                    "              date" +
                    "              user {" +
                    "                login" +
                    "                name" +
                    "              }" +
                    "            }" +
                    "          }" +
                    "        }" +
                    "      }";

    public static final String PR_GRAPHQL_COMMENTS =
            "      comments(first: $comments) {" +
                    "        totalCount" +
                    "        nodes {" +
                    "          bodyText" +
                    "          author {" +
                    "            login" +
                    "          }" +
                    "          createdAt" +
                    "          updatedAt" +
                    "        }" +
                    "      }";

    public static final String PR_GRAPHQL_REVIEWS =
            "      reviews(first: 100) {" +
                    "        totalCount" +
                    "        nodes {" +
                    "          id" +
                    "          bodyText" +
                    "          state" +
                    "          author {" +
                    "            login" +
                    "          }" +
                    "          createdAt" +
                    "          updatedAt" +
                    "        }" +
                    "      }";

    public static final String PR_GRAPHQL_END =
            "    }" +
            "  }" +
            "}";
}