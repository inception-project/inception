Essential Database Naming Conventions (and Style)

Source: http://justinsomnia.org/2003/04/essential-database-naming-conventions-and-style/

style

- use lowercase characters: eliminates question of proper case as well as errors related to 
-- case-sensitivity speeds typing rate and accuracy
-- differentiates table and column names from uppercase SQL keywords
- separate words and prefixes with underlines, never use spaces
-- promotes readability (e.g. book_name vs. bookname)
-- avoid having to bracket names (e.g. [book name] or `book name`)
-- offers greater platform independence
- avoid using numbers
-- may be a sign of poor normalization, hinting at the need for a many-to-many relationship