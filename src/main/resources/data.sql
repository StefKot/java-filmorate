MERGE INTO Genres KEY (id, name) VALUES (1, 'Комедия');
MERGE INTO Genres KEY (id, name) VALUES (2, 'Драма');
MERGE INTO Genres KEY (id, name) VALUES (3, 'Мультфильм');
MERGE INTO Genres KEY (id, name) VALUES (4, 'Триллер');
MERGE INTO Genres KEY (id, name) VALUES (5, 'Документальный');
MERGE INTO Genres KEY (id, name) VALUES (6, 'Боевик');

MERGE INTO MPA KEY (id, name) VALUES (1, 'G');
MERGE INTO MPA KEY (id, name) VALUES (2, 'PG');
MERGE INTO MPA KEY (id, name) VALUES (3, 'PG-13');
MERGE INTO MPA KEY (id, name) VALUES (4, 'R');
MERGE INTO MPA KEY (id, name) VALUES (5, 'NC-17');