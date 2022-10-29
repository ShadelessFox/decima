import logging
import sys


def get_logger(name) -> logging.Logger:
    logger = logging.getLogger(name)
    logger.handlers.clear()
    handler = logging.StreamHandler(sys.stdout)
    logger.setLevel(logging.DEBUG)
    handler.setFormatter(logging.Formatter("[%(levelname)s]--[%(name)s]: %(message)s"))
    logger.addHandler(handler)
    return logger
