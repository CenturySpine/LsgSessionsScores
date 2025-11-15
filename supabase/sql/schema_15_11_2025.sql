-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.app_roles
(
    id         bigint GENERATED ALWAYS AS IDENTITY NOT NULL UNIQUE,
    role_name  text                                NOT NULL,
    role_level bigint                              NOT NULL,
    CONSTRAINT app_roles_pkey PRIMARY KEY (id)
);
CREATE TABLE public.app_user
(
    id           uuid                     NOT NULL,
    email        text,
    display_name text,
    avatar_url   text,
    provider     text,
    created_at   timestamp with time zone NOT NULL DEFAULT now(),
    updated_at   timestamp with time zone NOT NULL DEFAULT now(),
    role_id      bigint                   NOT NULL DEFAULT '2'::bigint,
    CONSTRAINT app_user_pkey PRIMARY KEY (id),
    CONSTRAINT app_user_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.app_roles (id)
);
CREATE TABLE public.app_versions
(
    version           text    NOT NULL,
    is_current        boolean NOT NULL DEFAULT false,
    download_link     text,
    release_note_link text,
    CONSTRAINT app_versions_pkey PRIMARY KEY (version)
);
CREATE TABLE public.cities
(
    id   bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    name text                                NOT NULL,
    CONSTRAINT cities_pkey PRIMARY KEY (id)
);
CREATE TABLE public.game_zones
(
    id      bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    name    text                                NOT NULL,
    cityid  bigint                              NOT NULL DEFAULT 1,
    user_id uuid                                NOT NULL,
    CONSTRAINT game_zones_pkey PRIMARY KEY (id),
    CONSTRAINT fk_game_zones_city FOREIGN KEY (cityid) REFERENCES public.cities (id)
);
CREATE TABLE public.holes
(
    id            bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    name          text                                NOT NULL,
    gamezoneid    bigint                              NOT NULL,
    description   text,
    distance      integer,
    par           integer                             NOT NULL,
    startphotouri text,
    endphotouri   text,
    user_id       uuid                                NOT NULL,
    CONSTRAINT holes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_holes_game_zone FOREIGN KEY (gamezoneid) REFERENCES public.game_zones (id)
);
CREATE TABLE public.played_hole_scores
(
    id           bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    playedholeid bigint                              NOT NULL,
    teamid       bigint                              NOT NULL,
    strokes      integer                             NOT NULL,
    CONSTRAINT played_hole_scores_pkey PRIMARY KEY (id),
    CONSTRAINT fk_played_hole_scores_played_hole FOREIGN KEY (playedholeid) REFERENCES public.played_holes (id),
    CONSTRAINT fk_played_hole_scores_team FOREIGN KEY (teamid) REFERENCES public.teams (id)
);
CREATE TABLE public.played_holes
(
    id         bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    sessionid  bigint                              NOT NULL,
    holeid     bigint                              NOT NULL,
    gamemodeid integer                             NOT NULL,
    position   integer                             NOT NULL,
    user_id    uuid                                NOT NULL,
    CONSTRAINT played_holes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_played_holes_session FOREIGN KEY (sessionid) REFERENCES public.sessions (id),
    CONSTRAINT fk_played_holes_hole FOREIGN KEY (holeid) REFERENCES public.holes (id)
);
CREATE TABLE public.players
(
    id       bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    name     text                                NOT NULL,
    photouri text,
    cityid   bigint                              NOT NULL DEFAULT 1,
    user_id  uuid                                NOT NULL,
    CONSTRAINT players_pkey PRIMARY KEY (id)
);
CREATE TABLE public.scoring_modes
(
    id          integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    name        text                                 NOT NULL,
    description text                                 NOT NULL,
    CONSTRAINT scoring_modes_pkey PRIMARY KEY (id)
);
CREATE TABLE public.sessions
(
    id            bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    datetime      text                                NOT NULL,
    enddatetime   text,
    sessiontype   text                                NOT NULL,
    scoringmodeid integer                             NOT NULL,
    gamezoneid    bigint                              NOT NULL,
    comment       text,
    isongoing     boolean                             NOT NULL DEFAULT false,
    weatherdata   jsonb,
    cityid        bigint                              NOT NULL DEFAULT 1,
    user_id       uuid                                NOT NULL,
    CONSTRAINT sessions_pkey PRIMARY KEY (id),
    CONSTRAINT fk_sessions_scoring_mode FOREIGN KEY (scoringmodeid) REFERENCES public.scoring_modes (id),
    CONSTRAINT fk_sessions_city FOREIGN KEY (cityid) REFERENCES public.cities (id),
    CONSTRAINT fk_sessions_game_zone FOREIGN KEY (gamezoneid) REFERENCES public.game_zones (id)
);
CREATE TABLE public.teams
(
    id        bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    sessionid bigint                              NOT NULL,
    player1id bigint                              NOT NULL,
    player2id bigint,
    user_id   uuid                                NOT NULL,
    CONSTRAINT teams_pkey PRIMARY KEY (id),
    CONSTRAINT fk_teams_session FOREIGN KEY (sessionid) REFERENCES public.sessions (id),
    CONSTRAINT teams_player1id_fkey FOREIGN KEY (player1id) REFERENCES public.players (id),
    CONSTRAINT teams_player2id_fkey FOREIGN KEY (player2id) REFERENCES public.players (id)
);
CREATE TABLE public.user_player_link
(
    user_id    uuid                     NOT NULL,
    player_id  bigint UNIQUE,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT user_player_link_pkey PRIMARY KEY (user_id)
);