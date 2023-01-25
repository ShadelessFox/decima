import logging


def get_logger(name) -> logging.Logger:
    logging.basicConfig(format="[%(levelname)s]--[%(name)s]: %(message)s", level=logging.INFO)
    logger = logging.getLogger(name)
    # handler = logging.StreamHandler(sys.stdout)
    # logger.setLevel(logging.DEBUG)
    # handler.setFormatter(logging.Formatter("[%(levelname)s]--[%(name)s]: %(message)s"))
    # logger.addHandler(handler)
    return logger
