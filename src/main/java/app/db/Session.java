package app.db;

public final class Session {
    private static Long userId;
    private static String username;
    private static Role role;

    private Session() {}

    public static void set(long id, String u, Role r){
        userId = id;
        username = u;
        role = r;
    }

    public static void clear(){
        userId = null;
        username = null;
        role = null;
    }

    public static boolean isLoggedIn(){
        return userId != null;
    }

    public static Long userId(){
        return userId;
    }

    public static long userIdOrThrow(){
        if (userId == null) {
            throw new IllegalStateException("Not logged in");
        }
        return userId;
    }

    public static String username(){
        return username;
    }

    public static Role role(){
        return role;
    }
}
